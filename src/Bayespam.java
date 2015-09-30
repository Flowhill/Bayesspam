import java.io.*;
import java.util.*;

public class Bayespam
{

    // This defines the two types of messages we have.
    static enum MessageType
    {
        NORMAL, SPAM
    }

    // This a class with two counters (for regular and for spam)
    static class Multiple_Counter
    {
        int counter_spam    = 0;
        int counter_regular = 0;

        // Increase one of the counters by one
        public void incrementCounter(MessageType type)
        {
            if ( type == MessageType.NORMAL ){
                ++counter_regular;
            } else {
                ++counter_spam;
            }
        }

        public int getRegular(){
            return counter_regular;
        }
        public int getSpam(){
            return counter_spam;
        }


    }

    // Listings of the two subdirectories (regular/ and spam/)
    private static File[] listing_regular = new File[0];
    private static File[] listing_spam = new File[0];

    // A hash table for the vocabulary (word searching is very fast in a hash table)
    private static Hashtable <String, Multiple_Counter> vocab = new Hashtable <String, Multiple_Counter> ();


    // Add a word to the vocabulary
    private static void addWord(String word, MessageType type)
    {
        Multiple_Counter counter = new Multiple_Counter();

        ///ignore short words and punctuation:
        if ( word.length() < 4 ) return;

        if ( vocab.containsKey(word) ){                  // if word exists already in the vocabulary..
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
        if ( dir_listing.length != 2 )
        {
            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }

        listing_regular = dir_listing[0].listFiles();
        listing_spam    = dir_listing[1].listFiles();
    }


    // Print the current content of the vocabulary
    private static double[] printVocab(double[] nWords, int nWordsRegular, int nWordsSpam)
    {
        Multiple_Counter counter = new Multiple_Counter();

        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
        {
            String word;

            word = e.nextElement();
            counter  = vocab.get(word);

            System.out.println( word + " | in regular: " + counter.counter_regular +
                    " in spam: "    + counter.counter_spam);
            /// Calculate the sum of counter_regular and counter_spam over all the words in the vocabulary
            nWordsRegular += counter.counter_regular;
            nWordsSpam += counter.counter_spam;
        }
        nWords[1] = nWordsRegular;
        nWords[2] = nWordsSpam;
        nWords[3] = counter.counter_regular/nWordsRegular;
        nWords[4] = counter.counter_regular/nWordsSpam;
        return nWords;

    }


    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not
    private static int readMessages(MessageType type)
            throws IOException
    {
        File[] messages = new File[0];

        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }
        for (int i = 0; i < messages.length; ++i)
        {
            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String word;

            while ((line = in.readLine()) != null)                      // read a line
            {
                ///convert to lower case:
                line = line.toLowerCase();
                ///eliminate unwanted characters:
                line = line.replaceAll("[^a-z]"," ");

                StringTokenizer st = new StringTokenizer(line);         // parse it into words

                while (st.hasMoreTokens())                  // while there are still words left..
                {
                    addWord(st.nextToken(), type);                  // add them to the vocabulary
                }
            }

            in.close();
        }
        return messages.length;
    }

    private static double classifyMessages(MessageType type, double p_Regular, double p_Spam)
            throws IOException
    {
        /// Set the counter for regular and spam words in the vocabulary and their probabilities
        int classifiedRegular = 0, classifiedSpam = 0, totalWords = 1;
        double p_ClassRegular= p_Regular, p_ClassSpam = p_Spam;
        /// Set the counter for whether a message is classified as regular (1) or spam (2)
        double[] classification = new double[2];
        classification[1] = 0;
        classification[2] = 0;

        File[] messages = new File[0];

        if (type == MessageType.NORMAL){
            messages = listing_regular;
        } else {
            messages = listing_spam;
        }
        for (int i = 0; i < messages.length; ++i)
        {
            FileInputStream i_s = new FileInputStream( messages[i] );
            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
            String line;
            String word;

            while ((line = in.readLine()) != null)                      // read a line
            {
                ///convert to lower case:
                line = line.toLowerCase();
                ///eliminate unwanted characters:
                line = line.replaceAll("[^a-z]"," ");

                StringTokenizer st = new StringTokenizer(line);         // parse it into words

                while (st.hasMoreTokens())                  // while there are still words left..
                {
                    ///If the word is in the vocabulary, add 1 to either the regular or spam counter.
                    word = st.nextToken();
                    if(vocab.containsKey(word)) {
                        if(vocab.get(word).counter_regular >0) {
                            classifiedRegular++;
                        }else{
                            classifiedSpam++;
                        }
                    }
                    /// Calculate the a posteri probabilities
                    totalWords = classifiedRegular+classifiedSpam;
                    p_ClassRegular *= classifiedRegular/totalWords;
                    p_ClassSpam *= classifiedSpam/totalWords;

                }
            }
            ///Compare the logprobabilities to classify
            if(Math.log(p_ClassRegular) > Math.log(p_ClassSpam)) {
                classification[1]++;
            } else{
                classification[2]++;
            }

            in.close();
        }
        ///Return the percentage of messages that are correctly classified
        if(type == MessageType.NORMAL){
            return classification[1]/totalWords;
        } else {
            return classification[2]/totalWords;
        }
    }

    public static void main(String[] args)
            throws IOException
    {

        /// Initializing the probability variables
        int nWordsRegular = 0, nWordsSpam = 0, p_Regular_Msg, p_Spam_Msg;
        double p_classRegular = 0, p_classSpam = 0;
        /// classification is an array that will hold the % of correctly classified words
        double[] classification = new double[2];
        classification[1] = 0;
        classification[2] = 0;

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

        // Read the e-mail messages         /// Initializing the a priori variables
        int nMessagesRegular = readMessages(MessageType.NORMAL);
        int nMessagesSpam = readMessages(MessageType.SPAM);

        /// Calculate total messages and probabilities
        int nMessagesTotal = nMessagesRegular + nMessagesSpam;
        double p_Regular = Math.log((double)nMessagesRegular/(double)nMessagesTotal);
        double p_Spam = Math.log((double)nMessagesSpam/(double)nMessagesTotal);


        // Print out the hash table /// and create an array to save the conditional variables
        double[] nWords = new double[5];
        nWords = printVocab(nWords, nWordsRegular, nWordsSpam);

        /// Calculate the class conditional likelihoods

        nWordsRegular = (int)nWords[1];
        nWordsSpam = (int)nWords[2];
        p_classRegular = Math.log(nWords[3]);
        p_classSpam = Math.log(nWords[4]);

        /// Preventing probabilities from being 0
        double zeroProbAvoider = Math.log(1/(nWordsRegular+nWordsSpam));
        if(p_Regular == 0){ p_Regular = zeroProbAvoider;}
        if(p_Spam == 0){ p_Spam = zeroProbAvoider;}
        if(p_classRegular == 0){ p_classRegular = zeroProbAvoider;}
        if(p_classSpam == 0){ p_classSpam = zeroProbAvoider;}

        // Location of the directory (the path) taken from the cmd line (first arg)
        File dir_testlocation = new File( args[1] );

        // Check if the cmd line arg is a directory
        if ( !dir_testlocation.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg 2 not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        // Initialize the regular and spam lists
        listDirs(dir_testlocation);

        classification[1] = classifyMessages(MessageType.NORMAL, p_Regular, p_Spam);
        classification[2] = classifyMessages(MessageType.SPAM, p_Regular, p_Spam);
        System.out.println("Percentage of correctly classified Regular messages = " +classification[1]+"%");
        System.out.println("Percentage of correctly classified Spam messages = " +classification[2]+"%");




        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages
        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive
        // 3) Conditional probabilities must be computed for every word
        // 4) A priori probabilities must be computed for every word
        // 5) Zero probabilities must be replaced by a small estimated value
        // 6) Bayes rule must be applied on new messages, followed by argmax classification
        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms))
        // 8) Improve the code and the performance (speed, accuracy)
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
    }
}