package com.example.project;

import java.util.*;
import java.util.regex.Pattern;

public class Summarizer
{
    public Summarizer(){}//default constructor

    private Map<String, Integer> getWordCounts(String text)
    {
        Map<String,Integer> allWords = new HashMap<String, Integer>();

        int count;
        int singleIncrement = 0;

        text.trim();
        String[] words = text.split("\\s+");

        for(int i = 0; i < words.length; i++)
        {
            count = 0;

            if(allWords.containsKey(words[i]))
            {
                allWords.put(words[i], singleIncrement += 1);
            }
            else
            {
                allWords.put(words[i], count++);
            }
        }
        return allWords;
    }
    private Map<String,Integer> filterStopWords(Map<String, Integer> d)
    {
        String[] stop_words = { "a","able","about","after","all","also","am",
                "an","and","any","are","as","at","be","because","been","but","by","can","cannot","could","did",
                "do","does","either","else","ever","every","for","from","get","got","had","has","have","he","her","hers","him","his","how","I",
                "if","in","into","is","it","its","just","let","like","likely","may","me",
                "might","most","must","my","neither","no","nor","not","of","off",
                "often","on","only","or","other","our","own","said","say","says","she",
                "should","so","some","than","that","the","their","them","then","there",
                "these","they","this","they're","to","too","that's","us","was","we","were",
                "what","when","where","which","while","who","whom","why","will","with",
                "would","yet","you","your", "you're" };

        for(int i= 0; i < stop_words.length; i++)
        {
            if(d.containsKey(stop_words[i]))
            {
                d.remove(stop_words[i]);
            }
        }

        return d;
    }
    private List<String> sortByFreqThenDropFreq(Map<String,Integer> wordFrequencies)
    {
        List<String> sortedCollection = new ArrayList<String>(wordFrequencies.keySet());
        Collections.sort(sortedCollection);
        Collections.reverse(sortedCollection);
        return sortedCollection;
    }
    private String[] getSentences(String text)
    {
        text = text.replace("Mr.", "Mr").replace("Ms.", "Ms").replace("Dr.", "Dr").replace("Jan.", "Jan").replace("Feb.", "Feb")
                .replace("Mar.", "Mar").replace("Apr.", "Apr").replace("Jun.", "Jun").replace("Jul.", "Jul").replace("Aug.", "Aug")
                .replace("Sep.","Sep").replace("Spet.", "Sept").replace("Oct.", "Oct").replace("Nov.", "Nov").replace("Dec.", "Dec")
                .replace("St.", "St").replace("Prof.", "Prof").replace("Mrs.", "Mrs").replace("Gen.", "Gen")
                .replace("Corp.", "Corp").replace("Mrs.", "Mrs").replace("Sr.","Sr").replace("Jr.", "Jr").replace("cm.", "cm")
                .replace("Ltd.", "Ltd").replace("Col.", "Col").replace("vs.", "vs").replace("Capt.", "Capt")
                .replace("Univ.", "University").replace("Sgt.", "Sgt").replace("ft.","ft").replace("in.","in")
                .replace("Ave.", "Ave").replace("Univ.", "University").replace("Lt.", "Lt").replace("etc.", "etc").replace("mm.", "mm")
                .replace("\n\n", "").replace("\n", "").replace("\r", "");

        text = text.replaceAll("([A-Z])\\.", "$1");

        String pattern = "(?<!\\d)\\.(?!\\d)|(?<=\\d)\\.(?!\\d)|(?<!\\d)\\.(?=\\d)";
        Pattern pt = Pattern.compile(pattern);

        String[] sentences = pt.split(text);
        return sentences;
    }
    private String search(String[] sentences, String word)
    {
        String first_matching_sentence = null;
        for(int i = 0; i < sentences.length; i++)
        {
            if(sentences[i].contains(word))
            {
                first_matching_sentence = sentences[i];
            }
        }
        return first_matching_sentence;
    }
    public String Summarize(String text, int maxSummarySize)
    {
        if(text.equals("") || text.equals(" ") || text.equals("\n"))
        {
            String msg = "";
            return msg;
        }
        Map<String, Integer> wordFrequencies = getWordCounts(text);

        Map<String, Integer> filtered = filterStopWords(wordFrequencies);

        List<String> sorted = sortByFreqThenDropFreq(filtered);

        String[] sentences = getSentences(text);

        String firstSentence = sentences[0];
        String datePatternString = "(Monday|Tuesday|Wednesday|Thursday|Friday|Saturday)\\s\\d{1,2}\\s(January|February|March|April|May|June|July|August|September|October|November|December)\\s\\d{4}\\s\\d{1,2}\\.\\d{2}(\\sEST|\\sPST)";

        firstSentence = firstSentence.replace("Last modified on", "");
        firstSentence = firstSentence.replaceAll(datePatternString,"");


        List<String> setSummarySentences = new ArrayList<String>();
        for(String word : sorted)
        {
            String first_matching_sentence = search(sentences, word);
            if(!setSummarySentences.contains(first_matching_sentence))
                setSummarySentences.add(first_matching_sentence);
            if(setSummarySentences.size() == maxSummarySize)
            {
                break;
            }
        }

        Set<String> set = new LinkedHashSet<>(Arrays.asList(sentences));
        ArrayList<String> sentences_min = new ArrayList<>();
        sentences_min.addAll(set);

        String summary = "";
        if(!setSummarySentences.contains(firstSentence))
            summary = summary + "• " + firstSentence +  System.getProperty("line.separator") + System.getProperty("line.separator");

        for(String sentence : sentences_min)
        {
            if(setSummarySentences.contains(sentence))
            {
                summary = summary + "• " + sentence +  System.getProperty("line.separator") + System.getProperty("line.separator");
            }
        }
        return summary;
    }
}
