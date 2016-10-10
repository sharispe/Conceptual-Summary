#!/usr/bin/python

index_noun = "/data/WordNet/WordNet-3.1/dict/index.noun"
mapping = "/data/WordNet/WordNet-3.1/dict/index.noun.mapping"

print "Loading index"

f = open(mapping,'w')
for l in open(index_noun,'r'):
	
	if(l[0] == " "):
		continue
		
	data  = l.split();
	#print data
	label = data[0]
	nb_sense = int(data[2])
	out = label.replace("_"," ")
	out += "\t"
	for x in range(0, nb_sense):	
		code = data[len(data)-1-x]
		if(x!=0): out += ";"
		out += code
	f.write(out+"\n")


f.close()
