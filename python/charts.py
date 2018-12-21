# -*- coding: utf-8 -*-

# Libraries
import matplotlib.pyplot as plt
from math import pi

def radar(data, url):
	# number of variable
	categories = list(data["categories"])
	N = len(categories)

	# We are going to plot the first line of the data frame.
	# But we need to repeat the first value to close the circular graph:
	values = list(data["values"])
	values.append(values[0])

	# What will be the angle of each axis in the plot? (we divide the plot / number of variable)
	angles = [n / float(N) * 2 * pi for n in range(N)]
	angles += angles[:1]

	# Initialise the spider plot
	ax = plt.subplot(111, polar=True)

	# Draw one axe per variable + add labels labels yet
	plt.xticks(angles[:-1], categories, color='grey', size=10)
	
	ax.tick_params(axis='x', which='major', pad=15)
	
	# Set scale.
	sorted_values = list(data["values"])
	sorted_values.sort(reverse=True)
	
	scale_y = []
	scale_y_str = []
	if(sorted_values[0] > 80):
		scale_y = [20,40,60,80,100]
		scale_y_str = ["20","40","60","80","100"]
	elif(sorted_values[0] > 60):
		scale_y = [20,40,60,80]
		scale_y_str = ["20","40","60","80"]
	elif(sorted_values[0] > 40):
		scale_y = [20,40,60]
		scale_y_str = ["20","40","60"]
	else:
		scale_y = [20,40]
		scale_y_str = ["20","40"]
	
	# Draw ylabels
	ax.set_rlabel_position(0)
	plt.yticks(scale_y, scale_y_str, color="grey", size=6)
	plt.ylim(0,scale_y[-1])

	# Plot data
	ax.plot(angles, values, linewidth=1, linestyle='solid')

	# Fill area
	ax.fill(angles, values, 'b', alpha=0.1)
	plt.savefig(url)
	# plt.show()


def barPlot(data, url):
	fig, ax = plt.subplots(figsize=(10,10))
	plt.bar(data["categories"], data["values"])
	# Rotate x labels.
	ax.set_xticklabels(data["categories"], rotation=40)
	
	plt.savefig(url)
	# plt.show()

# data = {
# 	"categories": ["A", "B"],
# 	"values": [10, 15]
# }
# barPlot(data, "../data/xp/outputs/charts/barplot.png")

	
	