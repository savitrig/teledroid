#
# file: Makefile
# date: 2002-09-27 
#   by: GDB
# desc: Makefile for PIO paper

MAIN      = main

XDVIARGS   = -fg black -bg white

CLEANFILES = *~ *.aux *.log *.bbl *.blg *.dvi *.ps *.pdf

TEXD       = llncs 


all:	latex
	dvips -t letter -f ${MAIN} > ${MAIN}.ps
	ps2pdf ${MAIN}.ps > ${MAIN}.pdf
latex:
	TEXINPUTS=${TEXINPUTS}:${TEXD} latex ${MAIN}.tex
	TEXINPUTS=${TEXINPUTS}:${TEXD} bibtex ${MAIN}
	TEXINPUTS=${TEXINPUTS}:${TEXD} latex ${MAIN}.tex
	TEXINPUTS=${TEXINPUTS}:${TEXD} latex ${MAIN}.tex

bibtex:
	bibtex ${MAIN}

ps:	
	dvips -t letter -f ${MAIN} > ${MAIN}.ps

pdf:	
	ps2pdf ${MAIN}.ps > ${MAIN}.pdf

gv:
	gv ${MAIN} &

cleantest:
	ls -R ${CLEANFILES}

clean:
	rm -rf ${CLEANFILES}
	find ./ -name \*~ -exec rm -rf \{\} \;

