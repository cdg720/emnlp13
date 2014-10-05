import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Element;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.process.Morphology;

public class Experiment {
	static int freqTest = 0;
	static int freqTrain = 10;
	static int windowSize;// = 50;
	static int senseNum;// = 5;
	
	static int maxIter;
	static int burnIn;
	static double alpha;// = 0.02;
	static double beta;// = 0.1;
	
	static double power;
	
	static HashSet<String> puncs;
	static HashSet<String> stops;

	public static void main(String[] args) {
		String pPath = "../data/punctuation.txt";
		String sPath = "../data/smart_common_words.txt";
		puncs = new Words(pPath).getSet();
		stops = new Words(sPath).getSet();
		
		String[] kPath = {"../data/verbs.txt", "../data/nouns.txt"};
		
		power = Double.parseDouble(args[1]);
		alpha = Double.parseDouble(args[2]);
    beta = Double.parseDouble(args[3]);
		senseNum = Integer.parseInt(args[4]);
		windowSize = Integer.parseInt(args[5]);
		maxIter = Integer.parseInt(args[6]);
		burnIn = Integer.parseInt(args[7]);	
    
		
		System.out.println("freqTest: " + freqTest);
		System.out.println("freqTrain: " + freqTrain);
		System.out.println("power: " + power);
		System.out.println("windowSize: " + windowSize);
		System.out.println("senseNum: " + senseNum);
		System.out.println("alpha: " + alpha);
		System.out.println("beta: " + beta);
		System.out.println("maxIter: " + maxIter);
		System.out.println("burnIn: " + burnIn);
    System.out.println();

		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(args[0]));
			for (int i = 0; i < 2; i++) {
				List<String> keys = new Words(kPath[i]).getList();
				for (String key : keys) {
					System.out.println(key);					
					List<Integer> counts = new ArrayList<Integer>();
					List<List<Integer>> dist2 = new ArrayList<List<Integer>>();
					String trainPath = concatenate(args[8], key, i);
					List<List<String>> trainWords
						= preprocessCorpus(trainPath, key, freqTrain, counts, dist2, i);
					List<Integer> dummy = new ArrayList<Integer>();
					List<List<Integer>> dist = new ArrayList<List<Integer>>();

					String testPath = concatenate(args[9], key, i);
					List<List<String>> testWords 
						= preprocessCorpus(testPath, key, freqTest, dummy, dist, i);
					metaGibbs(testWords, key, i, bw, trainWords, counts, dist, dist2);
				}
				bw.flush();
			}			
			bw.close();
		} catch(IOException e) {
			System.err.println(e.getMessage());
		}	
	}
	
	public static List<List<String>> bagOfWords(List<List<String>> d, 
			String target, int freq, List<Integer> counts, 
			List<List<Integer>> dist, int y) {

		List<List<String>> words = new ArrayList<List<String>>();
		Morphology m = new Morphology();
		
		// positions of target word
		List<List<Integer>> pos = new ArrayList<List<Integer>>();
		// frequencies of words
		HashMap<String, Integer> count = new HashMap<String, Integer>();
		for (List<String> lines : d) {
			int p = 0;
			List<Integer> tmp = new ArrayList<Integer>();
			for (String word : lines) {
				p++;
				String key = word.toLowerCase();
				if (key.equals(target) || key.startsWith(target)) { // is this target?
					tmp.add(p-1);
					continue;
				}				
				if (!key.contains("_")) {
					key = m.stem(key);
				}
				if (key == null) continue; // sanity check
				if (key.startsWith(target) || key.equals(target)) { // never happens?
					tmp.add(p-1);
					continue;
				}
				if (puncs.contains(key) || stops.contains(key)) { // stop or punc
					continue;
				}
				if (count.containsKey(key)) { // record frequencies
					count.put(key, count.get(key) + 1);						
				} else {
					count.put(key, 1);
				}
			}
			pos.add(tmp);
			counts.add(tmp.size());
		}
		
		for (int j = 0; j < d.size(); j++) {
			List<String> list = new ArrayList<String>();
			List<Integer> dList = new ArrayList<Integer>();
			int p = 0;
			for (String word: d.get(j)) {
				p++;
				String key = word.toLowerCase();
				if (key.equals(target) || key.startsWith(target)) { // target 
					continue;
				}
				if (!key.contains("_")) {
					key = m.stem(key);
				}
				if (puncs.contains(key) || stops.contains(key) || key.startsWith(target)) {
					continue;
				}
				if (count.get(key) > freq) {
          int x = p-1;
          int min = Integer.MAX_VALUE;
          for (Integer in : pos.get(j)) {
            if (x <= in + windowSize && x >= in - windowSize) {
              if (min > Math.abs(in - x)) {
                min = Math.abs(in - x);
              }
            }
          }
          if (min != Integer.MAX_VALUE) {
            list.add(key);
            dList.add(min);
          }
        }
			}
			dist.add(dList);			
			words.add(list);
		}
		return words;
	}		

	public static String concatenate(String s, String key, int i) {
    if (!s.endsWith("/")) s = s + "/";

		if (i == 1) s += "nouns/";
    else s += "verbs/";

		s += key;

		if (i == 1) s += ".n.xml";
    else s += ".v.xml";

		return s;
	}		
	
	public static <T> HashMap<T, Double> langMod(List<List<T>> c) {
		HashMap<T, Double> langMod = new HashMap<T, Double>();
		double total = 0;
		for (List<T> d : c) {
			for (T w : d) {
				if (langMod.containsKey(w)) langMod.put(w, langMod.get(w) + 1);
				else langMod.put(w, 1d);
			}
			total += d.size();
		}
		for (Map.Entry<T, Double> en : langMod.entrySet()) {
			langMod.put(en.getKey(), en.getValue() / total);
		}
		return langMod;
	}	
	
	public static void metaEM(List<List<String>> testWords, String key, 
			int i, BufferedWriter bw, List<List<String>> trainWords,
			List<HashMap<String, Double>> clusters, List<Double> td,
			List<List<Integer>> dist, List<List<Integer>> dist2) {
		
		// dist is for test data
		
		List<String> senses = new ArrayList<String>();
		for (int j = 1; j <= clusters.size(); j++) senses.add("" + j);
		
		HashMap<String, Double> langMod = langMod(trainWords);
		EM em = new EM(trainWords, senses, langMod, clusters, td, dist2, power, beta);
		em.run();
		
		String t = (i == 1) ? "n" : "v";		
		testEM(em.getTau(), senses, testWords, langMod.size(), bw, key, t, em.getSigma(), dist);
	}
	
		
	public static void metaGibbs(List<List<String>> testWords, String key, 
			int i, BufferedWriter bw, List<List<String>> trainWords, 
			List<Integer> counts, List<List<Integer>> dist,
			List<List<Integer>> dist2) {
		
		List<String> senses = new ArrayList<String>();
		for (int j = 1; j <= senseNum; j++) senses.add("" + j);
		
		List<List<Integer>> trainInt = new ArrayList<List<Integer>>();
		List<Integer> senseInt = new ArrayList<Integer>();
		List<String> inverse = new ArrayList<String>();
		preprocess(trainWords, senses, trainInt, senseInt, inverse);
		
		NaiveBayes nb = new NaiveBayes();
		nb.init(trainInt, senseInt, inverse, counts, alpha, beta);
		nb.run(maxIter, burnIn);
		nb.learnDistribution();
		
		List<HashMap<String, Double>> clusters = nb.getClusters();
		List<Double> td = nb.getTopicDistribution();
		metaEM(testWords, key, i, bw, trainWords, clusters, td, dist, dist2);
	}	
	
	public static void preprocess(List<List<String>> d, 
			List<String> s, List<List<Integer>> d2, 
			List<Integer> s2, List<String> inverseMap) {
		for (String sense : s) {
			s2.add(Integer.parseInt(sense)-1);
		}
		
		HashMap<String, Integer> map = new HashMap<String, Integer>();		
		
		int i = 0;
		for (List<String> line : d) {
			ArrayList<Integer> tmp = new ArrayList<Integer>();
			for (String w : line) {
				int g;
				if (map.containsKey(w)) {
					g = map.get(w);
				}
				else {
					g = i++;
					map.put(w, g);
					inverseMap.add(w);
				}
				tmp.add(g);
			}
			d2.add(tmp);
		}
	}	
	
	public static List<List<String>> preprocessCorpus(String path, 
			String key, int threshold, List<Integer> counts,  
			List<List<Integer>> dist, int i) {
		
		List<List<String>> test = new ArrayList<List<String>>();
		for (Element doc : new Misc().lines(path).getList()) {
			// it ignores unknown characters such as U+202C, U+200
			DocumentPreprocessor dp 
				= new DocumentPreprocessor(new StringReader(doc.getStringValue()));
			Iterator<List<HasWord>> itr = dp.iterator();
			List<String> tokenized = new ArrayList<String>();
			while(itr.hasNext()) {
				for (HasWord hw : itr.next()) tokenized.add(hw.toString());
			}
			test.add(tokenized);
		}
		int x = (i == 0) ? -1 : 1;
		return bagOfWords(test, key, threshold, counts, dist, x);
	}	
	
	public static void testEM(HashMap<Pair<String, String>, Double> tau, 
			List<String> senses, List<List<String>> testData, int types,
			BufferedWriter bw, String tar, String a, HashMap<String, Double> sigma,
			List<List<Integer>> dist) {
		
		List<HashMap<Pair<String, String>, Double>> normalized 
		= new ArrayList<HashMap<Pair<String, String>, Double>>();
	
		normalized.add(tau);
		for (int i = 2; i <= windowSize; i++) {
			HashMap<Pair<String, String>, Double> x 
				= new HashMap<Pair<String, String>, Double>();
			double z = 0;
			double expo = Math.pow(1. / i, power);
			for (double val : tau.values()) {
				z += Math.pow(val, expo);
			}
			for (Map.Entry<Pair<String, String>, Double> en : tau.entrySet()) {
				x.put(en.getKey(), en.getValue() / z);
			}
			normalized.add(x);
		}
		
		HashMap<String, Double> totals = new HashMap<String, Double>();
		for (String s : senses) totals.put(s, 0d);

		for (Map.Entry<Pair<String, String>, Double> en : tau.entrySet()) {
			String sense = en.getKey().getFirst();
			totals.put(sense, totals.get(sense) + en.getValue());
		}
		
		double safe = Math.pow(10, -6);
		double denom = 1 + (types + 1) * safe;
		
		for (int i = 0; i < testData.size(); i++) {
			List<Integer> dList = dist.get(i);
			double max = -Double.MAX_VALUE;
			int argmax = -1;
			for (int j = 0; j < senses.size(); j++) {
				double weight2 = 1;
				double p = weight2 * Math.log(sigma.get(senses.get(j)));
				for (int k = 0; k < testData.get(i).size(); k++) {
					String word = testData.get(i).get(k);					
					Pair<String, String> sw = new Pair<String, String>(senses.get(j), word);
					if (normalized.get(dList.get(k)-1).containsKey(sw)) {
						double val = normalized.get(dList.get(k)-1).get(sw);
						p += Math.log( (val + safe) / denom);
					} else {
						p += Math.log(safe / denom);
					}
				}
				if (p > max) {
					max = p;
					argmax = j;
				}
			}
			try {
				bw.write(tar + "." + a + " " + tar + "." + a + "." + (i+1) + " " + tar);
				bw.write("." + a + "." + senses.get(argmax) + "\n");
				bw.flush();
			} catch(IOException e) {
				System.err.println(e.getMessage());
			}
		}		
	}	
		
	public static void testGibbs(List<HashMap<String, Double>> clusters,
			List<List<String>> test, BufferedWriter bw, String key, int flag,
			List<Double> td, int J) {
		double[] total = new double[clusters.size()];
		for (int i = 0; i < clusters.size(); i++) {
			for (Double val : clusters.get(i).values()) total[i] += val;
		}
		
		String x;
		if (flag == 0) x = "v";
		else x = "n";
		
		HashSet<Integer> used = new HashSet<Integer>();
		
		for (int i = 0; i < test.size(); i++) {
			double max = -Double.MAX_VALUE;
			int argmax = -1;
			for (int j = 0; j < clusters.size(); j++) {
				HashMap<String, Double> clu = clusters.get(j);
				double tmp = Math.log(td.get(j));
				for (int k = 0; k < test.get(i).size(); k++) {
					String s = test.get(i).get(k);
					if (clu.containsKey(s)) {						
						double val = (clu.get(s) + beta) / (total[j] + beta * (J+1));
						tmp += Math.log(val);
					}
					else {						
						tmp += Math.log(beta / (total[j] + beta * (J+1)));
					}
				}
				if (max < tmp) {
					max = tmp;
					argmax = j;
				}
			}
			used.add(argmax);
			try {
				bw.write(key + "." + x + " " + key + "." + x + "." + (i+1) + " " + key);
				bw.write("." + x + "." + (argmax+1) + "\n");
				bw.flush();
			} catch(IOException e) {
				System.err.println(e.getMessage());
			}
		}
		System.out.println(used.size());
	}
}
