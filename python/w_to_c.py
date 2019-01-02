# -*- coding: utf-8 -*-

import sys, codecs, re, os
import word2vec
import charts
import unidecode
import spacy

# Word to concept mapping.

def levenshteinDistance(s, t):
	matrix = []
	for i in range(len(s)+1):
		vector = []
		for j in range(len(t)+1):
			vector.append(0)
		matrix.append(vector)
	
	for i in range(1,len(s)+1):
		matrix[i][0] = i
	
	for i in range(1,len(t)+1):
		matrix[0][i] = i
	
	for i in range(1, len(s)+1):
		for j in range(1, len(t)+1):
			if s[i-1] == t[j-1]:
				cost = 0
			else:
				cost = 1
			matrix[i][j] = min(matrix[i-1][j] + 1, matrix[i][j-1] + 1, matrix[i-1][j-1] + cost)

	return matrix, matrix[-1][-1]


def wordToConcept(words, concepts, conceptsWithEmbeddings, model):
	results = {"logs": [], "results": {}}
	
	# To avoid to check many times the same word.
	reverse_index = {}
	for w in words:
		word = ""
		if w in reverse_index:
			word = reverse_index[w]
			print("[d] %s -> %s" % (w,word))
			results["logs"].append("[d] %s -> %s" % (w,word))
		else:
			if w in concepts:
				word = w
				print("[m] %s -> %s" % (w,w.replace("_", " ")))
				results["logs"].append("[m] %s -> %s" % (w,w.replace("_", " ")))
			elif w in model.vocab:
				# Cosine similarity.
				best_score = 0.0
				best_index = 0
				for c in range(len(conceptsWithEmbeddings)):
					dist = model.distance(w, conceptsWithEmbeddings[c][0])
					# dist = [('chien', 'chat', 0.5780401676299438)]
					if dist[0][2] > best_score:
						best_score = dist[0][2]
						best_index = c
				# Take the distance max.
				word = conceptsWithEmbeddings[best_index][1]
				print("[e] %s -> %s" % (w,word))
				results["logs"].append("[e] %s -> %s" % (w,word))

			if word == "":
				approx = []
				# Approx terms.
				for c in concepts:
					# With words.
					m, s = levenshteinDistance(unidecode.unidecode(w), unidecode.unidecode(c) )
					if s <= 2:
						approx.append((c,s))
					# With their embeddings.
					if w in model.vocab:
						for e in embedded_concepts:
							m, s = levenshteinDistance(unidecode.unidecode(e), unidecode.unidecode(c) )
							if s <= 2:
								approx.append((c,s))

				if len(approx) > 0:
						approx = sorted(approx, key=lambda tup: tup[1])
						word = approx[0][0]
						print("[a] %s -> %s" % (w,word))
						results["logs"].append("[a] %s -> %s" % (w,word))
				else:
					print("[-] %s -> not_found" % w)
					results["logs"].append("[-] %s -> not_found" % w)
		
		if word != "":
			word = word.replace("_", " ")
			if word in results["results"]:
				results["results"][word] += 1
			else:
				results["results"][word] = 1
				reverse_index[w] = word

	return results
            

def getWords(url_words):
	words = []
	with codecs.open(url_words, "r", encoding="utf-8") as fin:
		content = fin.readlines()
		for line in content:
			m = re.match("(\D+)", line.strip())
			if m:
				word = m.group(1).strip()
				word = word.replace(" ", "_")
				words.append(word)
	print("Length of words %d" % len(words))
	return words

def getConcepts(url_concepts):
	concepts = set()
	with codecs.open(url_concepts, "r", encoding="utf-8") as fin:
		content = fin.readlines()
		for line in content:
			spl = line.strip().split("\t")
			if len(spl) == 2:
				concept = spl[1]
				concept = concept.replace(" ", "_")
				concepts.add(concept)
	print("Length of concepts %d" % len(concepts))
	return concepts

# Match concepts with embeddings.
def matchConcepts(concepts, model):
	results = {"logs": [], "results": []}
	for c in concepts:
		if c in model.vocab:
			# (c,c) -> matched concept & original concept.
			results["results"].append((c,c))
			results["logs"].append("[m] %s -> %s" % (c,c))
		else:
			# Lemmatize concepts to try to match with lemmatized embeddings (treeTaggered corpus).
			doc = nlp(c.replace("_", " "))
			lemmas = []
			for token in doc:
				lemmas.append(token.lemma_)
			lemma = " ".join(lemmas)
			if lemma in model.vocab:
				# (c,c) -> matched concept & original concept.
				results["results"].append((lemma,c))
				results["logs"].append("[l] %s -> %s" % (c,lemma))
			else:
				results["logs"].append("[-] %s -> not_found" % c)
	return results


if __name__ == "__main__":
	print("*** Loading lemmatizer ***")
	nlp = spacy.load("fr")

	url_words = sys.argv[1]
	url_concepts = sys.argv[2]
	url_embeddings = sys.argv[3]
	url_taxonomy = sys.argv[4]
	url_output = sys.argv[5]
	url_img = os.path.join(url_output, "charts")

	words = getWords(url_words)
	concepts = getConcepts(url_concepts)
	print("*** Loading embeddings model ***")
	model = word2vec.load(url_embeddings)

	conceptsWithEmbeddings = matchConcepts(concepts, model)
	results = wordToConcept(words, concepts, conceptsWithEmbeddings["results"], model)

	with codecs.open(os.path.join(url_output, "labels.tsv"), "w", encoding="utf-8") as fout:
		for key in results["results"]:
			fout.write(key+"\t"+str(results["results"][key])+"\n")

	# Compute best resume.
	os.system('java -jar target/Cut_summary-0.1-jar-with-dependencies.jar %s %s %s /tmp/results.tmp' % (url_taxonomy, url_concepts, os.path.join(url_output, "labels.tsv")))
	os.system('tail -1 /tmp/results.tmp > %s' % "/tmp/best_resume.tsv")

	# Build & save graphs.
	data_format = {"categories": [], "values": []}
	total = 0
	best_resume_log = ""
	with open("/tmp/best_resume.tsv", "r") as fin:
		for line in fin.readlines():
			best_resume_log = line.strip()
			spl1 = line.strip().split("\t")
			if len(spl1) > 1:
				spl2 = spl1[1].split(" - ")
				for item in spl2:
					m = re.match("(\D+) \((.*)\).*", item)
					if m:
						data_format["categories"].append(m.group(1))
						data_format["values"].append(float(m.group(2)))
						total += float(m.group(2))
	# Percentage.
	for i in range(len(data_format["values"])):
		data_format["values"][i] = (data_format["values"][i] * 100) / total
	print(data_format)
	charts.radar(data_format, os.path.join(url_img, "radar.png"))
	charts.barPlot(data_format, os.path.join(url_img, "barplot.png"))

	# Logs.
	with codecs.open(os.path.join(url_output, "logs.txt"), "w", encoding="utf-8") as fout:
		fout.write("\n#####################################\n# Match words to concepts\n#####################################\n")
		fout.write("*** Matching concepts with embeddings ([m] = exact matching, [l] = matching with lemmatized concepts, [-] = concept not found) ***\n")
		for log in conceptsWithEmbeddings["logs"]:
			fout.write("\t%s\n" % log)
		fout.write("*** Matching words with concepts ([m] = exact matching, [e] = matching with embeddings, [d] = word already seen, [-] = matching not found) ***\n")
		for log in results["logs"]:
			fout.write("\t%s\n" % log)
		fout.write("*** Best resume ***\n")
		fout.write(best_resume_log)
	
	# java -jar target/Cut_summary-0.1-jar-with-dependencies.jar data/xp/taxo_xp_odeurs.owl data/xp/index_taxo_xp_odeurs.tsv python/output_list.tsv /tmp/results.tmp
	# python3.6 python/w_to_c.py data/xp/query_xp_odeurs.tsv data/xp/index_taxo_xp_odeurs.tsv data/xp/embeddings/frWac_no_postag_phrase_500_cbow_cut100.bin data/xp/taxo_xp_odeurs.owl data/xp/outputs

