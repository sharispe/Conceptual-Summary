#!/usr/bin/python

'''

	Conversion des fruit#n#1 vers leur ID dans wordnet 13155706
	
	> index.noun
	13155706 20 n 01 fruit 0 037 @ 11696293 n 0000 + 02405704 a 0101 + 10133766 n 0101 + 13156451 n 0101 + 01656273 v 0101 + 01656437 v 0101 + 00507753 v 0101 + 00055930 v 0101 ~ 07721676 n 0000 ~ 11657286 n 0000 ~ 11720748 n 0000 ~ 12056536 n 0000 ~ 12178548 n 0000 ~ 12183275 n 0000 ~ 12213851 n 0000 ~ 12288194 n 0000 ~ 12321962 n 0000 ~ 12641307 n 0000 ~ 12662851 n 0000 ~ 12665044 n 0000 ~ 12668548 n 0000 ~ 12671566 n 0000 ~ 12679242 n 0000 ~ 12758011 n 0000 ~ 13154372 n 0000 ~ 13156451 n 0000 ~ 13156591 n 0000 ~ 13158186 n 0000 ~ 13158449 n 0000 ~ 13159085 n 0000 ~ 13159619 n 0000 ~ 13159832 n 0000 ~ 13160259 n 0000 ~ 13161144 n 0000 ~ 13162192 n 0000 ~ 13171155 n 0000 ~ 13171369 n 0000 | the ripened reproductive body of a seed plant  


'''

def is_number(s):
    try:
        float(s)
        return True
    except ValueError:
        return False

index_noun = "/data/WordNet/WordNet-3.1/dict/index.noun"
benchmark = "/data/benchmarks/semantic_similarity/ConceptSim/RG_sense.txt"

print "Loading index"

idMap = {}

for l in open(index_noun,'r'):
	
	if(l[0] == " "):
		continue
		
	data  = l.split();
	#print data
	label = data[0]
	vpos = data[1]
	nb_sense = int(data[2])
	for x in range(0, nb_sense):	
		string_id = label+"#"+vpos+"#"+str(x+1)
		code = data[len(data)-1-x]
		#print string_id, "\t", code
		
		idMap[string_id] = code

print "size index: ",len(idMap)
		

for l in open(benchmark,'r'):
	data  = l.split();
	k1 = idMap[data[0]]
	k2 = idMap[data[1]]
	v = data[2]
	print k1+"\t"+k2+"\t"+v
	

