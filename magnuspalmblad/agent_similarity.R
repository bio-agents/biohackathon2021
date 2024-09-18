library(jsonlite)
library(stringr)
library(igraph)
library(RCy3)
library(ontologyIndex)
library(ontologySimilarity)
library(GraphAlignment)

setwd("C:/Users/nmpalmblad/Desktop")

# This is still work in progress, but does construct an igraph object from a bio.agents
# agent function description and uploads this to Cytoscape, as a single network or a
# collection of networks with one network per bio.agents function.

agents2graph <- function(bioagentsID) {
  agent <- read_json(paste0('https://bio.agents/api/agent/?bioagentsID="', bioagentsID, '"&format=json&page=1'))$list

  G <- make_empty_graph()

  nNodes <- 0
  for(i in 1:length(agent[[1]]$`function`)) {
    nInputs <- 0; nOperations <- 0; nOutputs <- 0
    if(length(agent[[1]]$`function`[[i]]$input)>0) {
      for(j in 1:length(agent[[1]]$`function`[[i]]$input)) {
       G <- add.vertices(G, 1, 
                          type = 'input',
                          term = agent[[1]]$`function`[[i]]$input[[j]]$data$term,
                          uri = agent[[1]]$`function`[[i]]$input[[j]]$data$uri)
        nInputs <- nInputs + 1
      }
    }

    if(length(agent[[1]]$`function`[[i]]$operation)>0) {
      for(j in 1:length(agent[[1]]$`function`[[i]]$operation)) {
        G <- add.vertices(G, 1, 
                          type = 'operation',
                          term = agent[[1]]$`function`[[i]]$operation[[j]]$term,
                          uri = agent[[1]]$`function`[[i]]$operation[[j]]$uri)
        nOperations <- nOperations + 1
      }
    }

    if(length(agent[[1]]$`function`[[i]]$output)>0) {
      for(j in 1:length(agent[[1]]$`function`[[i]]$output)) {
        G <- add.vertices(G, 1, 
                          type = 'output',
                          term = agent[[1]]$`function`[[i]]$output[[j]]$data$term,
                          uri = agent[[1]]$`function`[[i]]$output[[j]]$data$uri)
        nOutputs <- nOutputs + 1
      }
    }
  
    if(nInputs>0) {
      for(i in 1:nInputs) {
        for(j in 1:nOperations) G <- add.edges(G, nNodes+c(i,nInputs+j))
      }
    }
  
    if(nOutputs>0) {
      for(i in 1:nOperations) {
        for(j in 1:nOutputs) G <- add.edges(G, nNodes+c(nInputs+i,nInputs+nOperations+j))
      }
    }
    nNodes <- nNodes + nInputs + nOperations + nOutputs
  }

  return(G)
}

# separate inputs, operations and outputs (0 similarity?)
EDAM <- get_ontology('EDAM_1.25.obo', propagate_relationships = 'is_a', extract_tags = 'minimal')
edamSimilarity <- function(edam1, edam2) {
  ancestors1 <- get_ancestors(EDAM, edam1)
  ancestors2 <- get_ancestors(EDAM, edam2)
  pathLength <- length(ancestors1) + length(ancestors2) - 2*length(intersect(ancestors1,ancestors2)) - 1
  if(pathLength == 0) similarity <- 2 else similarity <- 1/pathLength
  if(pathLength == -1) similarity <- 2
  return(similarity/2)
}

edamSimilarity('data:3110', 'data:3110') # same = 1
edamSimilarity('data:3110', 'data:3111') # siblings = 0.25
edamSimilarity('data:3110', 'data:3117') # child-parent = 0.5
edamSimilarity('data:3110', 'data:2535') # cousins = 0.125
edamSimilarity('data:3110', 'data:0928') # nephew-uncle = 0.16666...

G1 <- agents2graph('uniprot_align')
G2 <- agents2graph('phmmer')

similarityMatrix <- matrix(nrow=gorder(G1)+gorder(G2), ncol=gorder(G1)+gorder(G2))
nodes <- str_replace(str_match(c(V(G1)$uri, V(G2)$uri), '[^/]*$'), '_', ':')
for(i in 1:length(nodes)) {
  for(j in 1:length(nodes)) {
   similarityMatrix[i,j] <- edamSimilarity(nodes[i], nodes[j])
  }
}

pinitial<-InitialAlignment(psize=15, r=similarityMatrix, mode="reciprocal")

AM1 <- as_adjacency_matrix(G1)
AM2 <- as_adjacency_matrix(G2)

lookupLink<-seq(-2,2,.5)
linkParams<-ComputeLinkParameters(AM1, AM2, pinitial, lookupLink)
lookupNode<-c(-.5,.5,1.5)
nodeParams<-ComputeNodeParameters(dimA=gorder(G1), dimB=gorder(G2), 
                                  similarityMatrix, pinitial, lookupNode)


AlignNetworks(AM1, AM2, P=pinitial, linkScore = linkParams$ls, 
              selfLinkScore=linkParams$ls, lookupLink=lookupLink, lookupNode=lookupNode,
              nodeScore1=nodeParams$s1, nodeScore0=nodeParams$s0,
              similarityMatrix, bStart = 0.1, bEnd=30, maxNumSteps=50)



f

#G1 <- agents2graph('hmmer3')
#G2 <- agents2graph('hmmer2')


# G3 <- agents2graph('hmmer3')



createNetworkFromIgraph(G, title="From igraph")

dG <- decompose.graph(G)

for(i in 1:length(dG)) {
  createNetworkFromIgraph(dG[[i]], title = paste('Subgraph', i))
}

attributes(G)

