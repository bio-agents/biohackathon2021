library(jsonlite)
library(stringr)
library(ggplot2)
library(forcats)
library(ggsci)
library(europepmc)
library(urlagents)
library(rworldmap)
library(RColorBrewer)

# This is just a sandbox created during BioHackathon 2021

# Example 1: get first credited person for 100 top agents for certain EDAM topic,
# extract and genderize first names, and visually compare topics. 
# N.B. This R script attempts to extract gender bias in the people credited
# in the bio.agents entries in different topics or agent collections. In doing
# this we are limited to the male/female genders as called by genderize.io from
# the first names only. This is in no way an endorsement of gender as a binary
# variable. Also importantly, the script does not store or report genders of
# individual contributors, only aggregate statistics.

# fetch top 100 bio.agents records for a topic:
topic2genders <- function(topic) {
  agents <- read_json(paste0('https://bio.agents/api/agent/?topic="', topic, '"&format=json&page=1'))$list
  
  for(i in 2:8) {agents <- c(agents, read_json(
    paste0('https://bio.agents/api/agent/?topic="',topic, '"&format=json&page=',i))$list)
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
  return(c(male, female, unknown))
}

evolutionary_biology <- topic2genders('evolutionary biology')
genomics <- topic2genders('genomics')
proteomics <- topic2genders('proteomics')
metabolomics <- topic2genders('metabolomics')

df <- data.frame(topic=c(rep('evolutionary biology',3), rep('genomics',3),
                         rep('proteomics',3), rep('metabolomics',3)),
                 gender=rep(c('male', 'female', 'unknown'),4),
                 sorting=c(3,3,3,3,1,1,1,1,2,2,2,2),
                 credits=c(evolutionary_biology,
                           genomics,
                           proteomics,
                           metabolomics))
pdf("topics_and_genders.pdf", width = 7, height = 7)
ggplot(data=df, aes(x=topic, y=credits, fill=fct_reorder(df$gender, df$sorting))) +
  geom_bar(stat="identity") +
  xlab('EDAM topic') +
  ylim(c(0,100)) +
  ylab("credits/100 bio.agents agents") +
  labs(fill='genderized') +
  scale_fill_nejm()
dev.off()

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


# Example 3: Map geographic distribution of contributers to bio.agents, by the
# email address of the first/primary credited person or team.

count <- read_json('https://bio.agents/api/t?format=json')$count
agents <- read_json('https://bio.agents/api/t?format=json&page=1')$list

for(i in 2:round(0.5+count/10)) {agents <- c(agents, read_json(
  paste0('https://bio.agents/api/t?format=json&page=',i))$list)
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

pdf("credits_by_country.pdf", width = 7, height = 4)
mapBubbles(countryMap, nameZSize="V2", nameZColour="GEO3major", addLegend=F, 
           addColourLegend=F, colourPalette=adjustcolor(brewer.pal(7,'Accent'), alpha.f = 0.5), 
           ylim=c(-43,67))
dev.off()




agents <- read_json('https://bio.agents/api/agent/?t&format=json&page=1')$list

for(i in 1:length(agent[[1]]$`function`)) {
  for(j in 1:length(agent[[1]]$`function`[[i]]$operation)) {
    G <- add.vertices(G, 1, 
                      type = 'operation',
                      term = agent[[1]]$`function`[[j]]$operation[[j]]$term,
                      uri = agent[[1]]$`function`[[j]]$operation[[j]]$uri)
  }
}


# Example 4: Operation co-occurrence map for VOSviewer

count <- read_json('https://bio.agents/api/t?format=json')$count
agents <- read_json('https://bio.agents/api/t?format=json&page=1')$list

for(i in 2:round(0.5+count/10)) {agents <- c(agents, read_json(
#for(i in 2:10) {agents <- c(agents, read_json(
  paste0('https://bio.agents/api/t?format=json&page=',i))$list)
}

entry <- ''
for(i in 1:length(agents)) {
  if(length(agents[[i]]$`function`)>0) {
    for(j in 1:length(agents[[i]]$`function`)) {
      if(length(agents[[i]]$`function`[[j]]$operation)>0) {
        for(k in 1:length(agents[[i]]$`function`[[j]]$operation)) {
          entry <- paste(entry, paste0('"8x', gsub(' ', '9x', agents[[i]]$`function`[[j]]$operation[[k]]$term), '8x"'), sep=' ')
        }
      }
    }
  }
  entry <- paste(entry,'\n')
}

write.table(entry, "operations.txt", row.names=F, col.names=F)
