library(jsonlite)
library(stringr)
library(ggplot2)
library(forcats)
library(ggsci)
library(europepmc)
library(urlagents)
library(rworldmap)

# This is just a sandbox created during BioHackathon 2021

# Example 1: get first credited person for 100 top agents for certain EDAM topic,
# extract and genderize first names, and visually compare topics. 
# N.B. This R script attempts to extract gender bias in the people credited
# in the bio.agents entries in different topics or agent collections. In doing
# this we are limited to the male/female genders as called by genderize.io from
# the first names only. This is in no way an endorsement of gender as a binary
# variable. Also importantly, the script does not store or report genders of
# individual contributors, only aggregate statistics.

# fetch bio.agents records:
agents <- read_json('https://bio.agents/api/agent/?topic="biodiversity"&format=json&page=1')$list

for(i in 2:8) {agents <- c(agents, read_json(
              paste0('https://bio.agents/api/agent/?topic="biodiversity"&format=json&page=',i))$list)
}

male <- 0
female <- 0
unknown <- 0

for(i in 1:length(agents)) {
  if(length(agents[[i]]$credit)>0) {
    name <- agents[[i]]$credit[[1]]$name
    firstName <- unlist(str_split(name, ' '))[1] 
    if(is.null(firstName) == FALSE) {
      genderize <- fromJSON(paste('https://api.genderize.io/?name=', 
                                firstName, sep=''))
      if(genderize$gender=='male' && genderize$probability>0.66) male=male+1
      if(genderize$gender=='female' && genderize$probability>0.66) female=female+1
      if(genderize$probability<=0.66) unknown=unknown+1
    }
  }
}

# topic=proteomics: 14 female, 74 male, 7 unknown (100+ hits)
# topic=metabolomics: 13 female, 59 male, 11 unknown (100+ hits)
# topic-genomics: 15 female, 67 male, 15 unknown (100+ hits)
# topic-biodiversity: 8 female, 33 male, 17 unknown (76 hits)

df <- data.frame(topic=c(rep('biodiversity',3), rep('genomics',3),
                         rep('proteomics',3), rep('metabolomics',3)),
                 gender=rep(c('female', 'unknown', 'male'),4),
                 sorting=c(1,1,1,1,2,2,2,2,3,3,3,3),
                 credits=c(8*100/76,17*100/76, 33*100/76,
                           15,15,67,
                           14,7,74,
                           13,11,59))
ggplot(data=df, aes(x=topic, y=credits, fill=fct_reorder(df$gender, df$sorting))) +
  geom_bar(stat="identity") +
  xlab('EDAM topic') +
  ylim(c(0,100)) +
  ylab("credits/100 bio.agents agents") +
  labs(fill='genderized') +
  scale_fill_startrek()


# Example 2: get first (main) publication for all bio.agents records with topic="proteomics"
# and use EuropePMC API to retrieve titles and abstracts for text mining.

count <- read_json('https://bio.agents/api/agent/?topic="proteomics"&format=json')$count
agents <- read_json('https://bio.agents/api/agent/?topic="proteomics"&format=json&page=1')$list

for(i in 2:round(0.5+count/10)) {agents <- c(agents, read_json(
  paste0('https://bio.agents/api/agent/?topic="proteomics"&format=json&page=',i))$list)
}

pubs <- c()
for(i in 1:length(agents)) {
  if(length(agents[[i]]$publication)>0) {
    pmid <- agents[[i]]$publication[[1]]$pmid
    pubs <- cbind(pubs, pmid)
  }
}

# write.table(as.numeric(pubs), "proteomics_pmids.txt", row.names=F, col.names=F)

titlesAbstracts <- ''
for(i in 1:length(pubs)) {
  pubMedResults <- epmc_search(paste0(query = 'EXT_ID:', pubs[i], '&resultType=core'))
  titlesAbstracts <- paste0(titlesAbstracts, pubMedResults$title, pubMedResults$abstractText, '\n')
}
write.table(titlesAbstracts, "PubMed_proteomics.txt", row.names=F, col.names=F)


count <- read_json('https://bio.agents/api/agent/?topic="genomics"&format=json')$count
agents <- read_json('https://bio.agents/api/agent/?topic="genomics"&format=json&page=1')$list

for(i in 2:round(0.5+count/10)) {agents <- c(agents, read_json(
  paste0('https://bio.agents/api/agent/?topic="genomics"&format=json&page=',i))$list)
}

pubs <- c()
for(i in 1:length(agents)) {
  if(length(agents[[i]]$publication)>0) {
    pmid <- agents[[i]]$publication[[1]]$pmid
    pubs <- cbind(pubs, pmid)
  }
}

# write.table(as.numeric(pubs), "genomics_pmids.txt", row.names=F, col.names=F)

titlesAbstracts <- ''
for(i in 1:length(pubs)) {
  pubMedResults <- epmc_search(paste0(query = 'EXT_ID:', pubs[i], '&resultType=core'))
  titlesAbstracts <- paste0(titlesAbstracts, pubMedResults$title, pubMedResults$abstractText, '\n')
}
write.table(titlesAbstracts, "PubMed_genomics.txt", row.names=F, col.names=F)


# Example 3 - geographic

count <- read_json('https://bio.agents/api/agent/?topic="genomics"&format=json')$count
agents <- read_json('https://bio.agents/api/agent/?topic="genomics"&format=json&page=1')$list

for(i in 2:round(0.5+count/10)) {agents <- c(agents, read_json(
  paste0('https://bio.agents/api/agent/?topic="genomics"&format=json&page=',i))$list)
}

emails <- c()
for(i in 1:length(agents)) {
  if(length(agents[[i]]$credit)>0) {
    if(length(agents[[i]]$credit[[1]]$email)>0) {
      email <- agents[[i]]$credit[[1]]$email
      emails <- cbind(emails, email)
    }
  }
}

tlds <- tld_extract(unlist(str_split(emails, "@")))$tld

tlds[tlds == "edu"] <- "us" # assume .edu is US
tlds[tlds == "gov"] <- "us" # assume .gov is US

countries <- as.data.frame(table(tlds))

total<-sum(countries$Freq)

countryData <- as.data.frame(matrix(nrow=nrow(countries), ncol=2))
countryData[,1] <- data.matrix(toupper(countries$tlds))
countryData[,2] <- data.matrix(countries$Freq)
countryMap<-joinCountryData2Map(countryData, joinCode="ISO2", nameJoinColumn="V1")

mapCountryData(countryMap, nameColumnToPlot='V2', catMethod = exp(seq(from=0,
               to=log(max(countryData[,2])+1), length.out=100)),
               addLegend = FALSE, 
               mapTitle ='bio.agents credits by country (topic = genomics')

mapBubbles(countryMap, nameZSize="V2", nameZColour="GEO3major", addLegend=F, 
           addColourLegend=F, colourPalette='rainbow', ylim=c(-10,67))
