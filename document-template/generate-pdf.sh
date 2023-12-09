xelatex -interaction=nonstopmode ../document-template/master.tex
rm master.aux
rm master.log
mv master.pdf ../version-log/document$(python -c 'from sys import argv; print(len(argv[1].split("\n")) if argv[1] != "" else 0)' "$(ls ../version-log)").pdf