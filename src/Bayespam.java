import com.sun.org.apache.xalan.internal.xsltc.runtime.*;

import java.io.*;
import java.util.*;
import java.util.Hashtable;

public class Bayespam
{
    // This defines the two types of messages we have.
    enum MessageType
    {
        NORMAL, SPAM
    }

    // This a class with two counters (for regular and for spam)
    static class Word_Stats
    {
        int counter_spam    = 0;
        int counter_regular = 0;
        double likelihood_spam    = 0;
        double likelihood_regular = 0;

        // Increase one of the counters by one
        public void incrementCounter(MessageType type)
        {
            if ( type == MessageType.NORMAL ){
                ++counter_regular;
            } else {
                ++counter_spam;
            }
        }
    }


    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static Hashtable <String, Word_Stats> vocab = new Hashtable <> ();

    // Add a word to the vocabulary
    private static void addWord(String word, MessageType type)
    {
        Word_Stats counter = new Word_Stats();

        if ( vocab.containsKey(word) ){                 // if word exists already in the vocabulary..
            counter = vocab.get(word);                  // get the counter from the hashtable
        }
        counter.incrementCounter(type);                 // increase the counter appropriately

        vocab.put(word, counter);                       // put the word with its counter into the hashtable
    }


    // List the regular and spam messages
    private static void listDirs(File dir_location)
    {
        // List all files in the directory passed
        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        assert dir_listing != null;
        if ( dir_listing.length != 2 )
        {
            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }

        listing_regular = dir_listing[0].listFiles();
        listing_spam    = dir_listing[1].listFiles();
    }


    // Print the current content of the vocabulary
    private static void printVocab()
    {
        Word_Stats counter;

        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {
            String word;

            word = e.nextElement();
            counter  = vocab.get(word);

            System.out.println( word + " | in regular: " + counter.counter_regular +
                    " in spam: "    + counter.counter_spam);
        }
    }


    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not
    private static void readMessages(MessageType type)
            throws IOException
    {
        File[] messages;

        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }

        for (File message : messages) {
            FileInputStream i_s = new FileInputStream(message);
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String word;

            while ((line = in.readLine()) != null)                      // read a line
            {
                line = line.toLowerCase();                              ///convert to lower case

                line = line.replaceAll("[^a-z]", " ");                   ///eliminate unwanted characters

                StringTokenizer st = new StringTokenizer(line);         // parse it into words

                while (st.hasMoreTokens())                  // while there are still words left..
                {
                    word = st.nextToken();
                    if (word.length() >= 4) addWord(word, type);       ///only add long enough words
                }
            }

            in.close();
        }
    }

    public static void main(String[] args)
            throws IOException
    {
        // Location of the directory (the path) taken from the cmd line (first arg)
        File dir_location = new File( args[0] );

        // Check if the cmd line arg is a directory
        if ( !dir_location.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_location);

        // Read the e-mail messages
        readMessages(MessageType.NORMAL);
        readMessages(MessageType.SPAM);

        // Print out the hash table
        printVocab();

        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages

        ///Computing a priori class log probabilities:
        int nMessagesRegular = listing_regular.length;
        int nMessagesSpam = listing_spam.length;
        double lognTotal = Math.log( 2 + nMessagesRegular + nMessagesSpam);
        double Prior_Regular = Math.log( 1 + nMessagesRegular) - lognTotal;
        double Prior_Spam = Math.log( 1 + nMessagesSpam) - lognTotal;

        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive

        /// Removal of unwanted characters and case insensitivity are in function 'readMessages'.
        /// Short words are ignored in function 'addWord'.


        // 3) Conditional probabilities must be computed for every word
        double tuningParameter = 1;

        ///Counting total number of words in regular/spam:
        int nWordsRegular, nWordsSpam;        nWordsRegular = nWordsSpam = 0;
        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {
            String word = e.nextElement();
            nWordsRegular += vocab.get(word).counter_regular;
            nWordsSpam += vocab.get(word).counter_spam;
        }
        double smallValue = tuningParameter / (nWordsRegular + nWordsSpam);

        int counterRegular, counterSpam;
        Word_Stats x;
        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {
            String word = e.nextElement();
            x = vocab.get(word);

            ///Computing class conditional word likelihoods:

            x.likelihood_regular = Math.log( smallValue + (double)x.counter_regular / (double)(1 + nWordsRegular) ) ;
            x.likelihood_spam = Math.log( smallValue + (double)x.counter_spam / (double)(1 + nWordsSpam) ) ;
            vocab.put( word, x);

        }


        // 4) A priori probabilities must be computed for every word




        // 5) Zero probabilities must be replaced by a small estimated value

        // This is not necessary since we computed the probabilities correctly
        // which never gives us zero probabilities in the first place.
        // The 'tuningParameter' can be adjusted as well.


        // 6) Bayes rule must be applied on new messages, followed by argmax classification

        Word_Stats counter = new Word_Stats();
        for (MessageType type : MessageType.values())
        {
            File[] messages; if (type == MessageType.NORMAL)
            {messages = listing_regular;} else {messages = listing_spam;}

            for (File message : messages) {
                FileInputStream i_s = new FileInputStream(message);
                BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
                String line, word;

                double Bayes_Factor = Prior_Regular - Prior_Spam;           ///initializing the Bayes factor

                while ((line = in.readLine()) != null)                      // read a line
                {
                    line = line.toLowerCase();                              ///convert to lower case
                    line = line.replaceAll("[^a-z]", " ");                  ///eliminate unwanted characters
                    StringTokenizer st = new StringTokenizer(line);         // parse it into words
                    while (st.hasMoreTokens()) {
                        word = st.nextToken();
                        if (word.length() >= 4) {
                            x = vocab.get(word);
                            Bayes_Factor += x.likelihood_regular - x.likelihood_spam;    ///updating with Bayes rule
                        }
                    }

                }

                ///Classification Counter:
                if (Bayes_Factor >= 0)
                    counter.incrementCounter(MessageType.NORMAL);
                else
                    counter.incrementCounter(MessageType.SPAM);
                in.close();
            }

            ///Return the percentage of messages that are correctly classified
            if(type == MessageType.NORMAL){
                System.out.println("Ratio of correctly classified Regular messages = "
                        +100*counter.counter_regular/messages.length+"%");
            } else {
                System.out.println("Ratio of correctly classified Spam messages = "
                        +100*counter.counter_spam/messages.length+"%");
            }
        }

        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms))



        // 8) Improve the code and the performance (speed, accuracy)
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
    }
}