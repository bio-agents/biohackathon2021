# About 

Cascabel (https://doi.org/10.3389/fgene.2020.489357) is a variable pipeline for amplicon sequence data analysis. 

It makes an interesting use case for workflow work with bio.agents/EDAM and APE, as it already foresees several variants of the main workflow, and potentially more can be found when taking to account the full content of bio.agents.

Here we document the work on bringing it all together. 

# Agents

The agents used in Cascabel are listed in the figure/table at https://www.frontiersin.org/files/Articles/489357/fgene-11-489357-HTML/image_m/fgene-11-489357-t001.jpg

Custom scripts are obviously not available in bio.agents (here's the shim discussion again!), first assessment of the other agents listed: 

| **Agent**  | **in bio.agents?**  | **bio.agents ID**  | **functional annotation?**  |
|-----------|--------------------|-------------------|-----------------------------|
| FastQC | yes | bioagents:fastqc | yes |
| PEAR | yes | bioagents:pear | no |
| QIIME | yes | bioagents:qiime | no |
| QIIME | yes | bioagents:qiime2 | no |
| Mothur | no | - | - |
| usearch61 | no | - | - |
| VSEARCH | yes | bioagents:vsearch | yes | 
| Cutadapt | yes | bioagents:cutadapt | yes | 
| Cutadapt 1.12 | yes | bioagents:cutadapt_1.12 | no |
| CD-HIT | yes | bioagents:cd-hit | yes |
| SUMACLUST | no | - | - |
| Swarm | yes | bioagents:swarm | yes |
| UCLUST | no | - | - | 
| trie | no | - | - |
| SortMeRna | yes | bioagents:sortmerna | no |
| DADA2 | yes | bioagents:dada2 | no | 
| BLAST | yes | bioagents:blast | no |
| pynast | no | - | - |
| MAFFT | yes | bioagents:MAFFT | yes | 
| Infernal | yes | bioagents:infernal | yes | 
| ClustalW | yes | bioagents:clustalw | yes | 
| MUSCLE | yes | bioagents:muscle | yes | 
| RAxML | yes | bioagents:raxml | no | 
| FastTree | yes | bioagents:fasttree | no | 
| Krona | yes | bioagents:krona | no | 

Next steps: 
* add the missing agents to bio.agents
* check and if necessary improve the functional annotations
* ...
* try to (re-)create the pipeline with APE (probably more steps needed before this works)









