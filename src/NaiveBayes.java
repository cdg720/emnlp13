import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;


public class NaiveBayes {
	List<List<Integer>> docs;
	List<Integer> senses;
	List<String> inverse;
	List<Integer> counts;
	
	List<HashMap<String, Double>> clusters; 
	List<Double> td;
	
	double alpha;
	double beta;
	int K; // # of senses
	int J; // # of types
	int[] d; 
	double[] ek;
	double[][] ekj;
	int[] map;
	int[][] topics;
	int[] total;
	double num; // # of samples
	
	double posterior;
	
	public NaiveBayes() {
		
	}
	
	public double computeLogPosterior() {
		double posterior = 0;
		posterior += Math.log(K * alpha); // 1
		posterior -= K * Gamma.logGamma(alpha); // 3
		posterior += K * Gamma.logGamma(beta * J); // 5
		posterior -= K * J * Gamma.logGamma(beta); // 7
		for (int i = 0; i < K; i++) {
			posterior += Gamma.logGamma(alpha + d[i]); // 2
			posterior -= Math.log(alpha + d[i]); // 4
		}
		for (int i = 0; i < K; i++) {
			double tmp = 0;
			tmp += beta * J + ek[i]; // 8
			for (int j = 0; j < J; j++) {
				posterior += Gamma.logGamma(beta + ekj[i][j]); // 6
			}
			posterior -= Gamma.logGamma(tmp);					
		}
		return posterior;
	}
	
	public void init(List<List<Integer>> docs, List<Integer> senses, 
			List<String> inverse, List<Integer> counts, double alpha, double beta) {
		this.docs = docs;
		this.senses = senses;
		this.inverse = inverse;
		this.counts = counts;
		
		this.alpha = alpha;
		this.beta = beta;
		
		K = senses.size();
		J = inverse.size();
		d = new int[K]; //number of documents assigned to topic		
		ekj = new double[K][J]; //e_{k,j}: times word j assigned to topic k
		ek = new double[K];
		map = new int[docs.size()]; //current topic assignment of a document
		topics = new int[docs.size()][K];
		num = 0;
		total = new int[K];
		
		clusters = new ArrayList<HashMap<String, Double>>();
		td = new ArrayList<Double>();
	}
	
	public List<HashMap<String, Double>> getClusters() {
		return clusters;
	}
	
	public double getPosterior() {
		return posterior;
	}
	
	public List<Double> getTopicDistribution() {
		return td;
	}
	
	public void learnDistribution() {
		for (int i = 0; i < K; i++) { // initialize distribution for each senses
			clusters.add(new HashMap<String, Double>());
		}
		
		for (int i = 0; i < map.length; i++) {
			List<Integer> doc = docs.get(i);
			for (int j = 0; j < K; j++) {
				if (topics[i][j] == 0) continue;
				double weight = topics[i][j] / num; 
				HashMap<String, Double> clu = clusters.get(j);
				for (Integer k : doc) {
					String key = inverse.get(k);
					if (clu.containsKey(key)) clu.put(key, clu.get(key) + weight);
					else clu.put(key, weight);
				}
				total[j] += doc.size() * weight;
			}			
		}	
		int sum = 0;
		for (Integer i : counts) sum += i;		
		double[] tmp = new double[K];
		for (int i = 0; i < docs.size(); i++) {
			for (int j = 0; j < K; j++) {
				double weight = topics[i][j] / num;
				tmp[j] += counts.get(i) * weight / sum;
			}
		}		
		for (int i = 0; i < K; i++) {
			td.add(tmp[i]);
		}
	}
	
	public void run(int maxIter, int burnIn) {		
		Random r = new Random();
		
		// counts # of word j in document m
		List<HashMap<Integer, Integer>> fmj 
			= new ArrayList<HashMap<Integer, Integer>>(docs.size());
		// stores distances of word j in document m
		for (int i = 0; i < docs.size(); i++) {
			fmj.add(new HashMap<Integer, Integer>());
		}
		// stores words that appeared in document m
		List<HashSet<Integer>> fajs = new ArrayList<HashSet<Integer>>();
		
		// random initialization
		for (int z = 0; z < docs.size(); z++) {
			HashSet<Integer> tmpList = new HashSet<Integer>();
			double[] probs = new double[K];
			for (int i = 0; i < K; i++) {
				probs[i] = 1. / K; // every sense is equally likely
			}
			int assign = flipACoin(r, probs); // randomly choose a sense
			map[z] = assign;
			d[assign]++; // assign document z to sense assign
			ek[assign] += docs.get(z).size(); 

			for (int jj = 0; jj < docs.get(z).size(); jj++) {
				int j = docs.get(z).get(jj);
				ekj[assign][j]++;
				if (fmj.get(z).containsKey(j)) {
					fmj.get(z).put(j, fmj.get(z).get(j)+1);
				}
				else {
					fmj.get(z).put(j, 1);
				}
				tmpList.add(j);
			}	
			fajs.add(tmpList); 
		}
		for (int x = 0; x < maxIter; x++) {
			if (x == maxIter-1) {
				posterior = computeLogPosterior();
				System.out.print("iter " + x + ": ");
				System.out.print("log posterior: " + posterior + " (");
				for (int i = 0; i < d.length; i++) {
					if (i != d.length - 1) System.out.print(d[i] + ", ");
					else System.out.println(d[i] + ")");
				}
			}			
			if (x > burnIn && x % 100 == 99) {
				for (int i = 0; i < map.length; i++) {
					topics[i][map[i]]++;
				}
				num++;
			}

			/* gibbs sampler */
			for (int z = 0; z < docs.size(); z++) {
				if (docs.get(z).size() == 0) {
				}
				//remove zth document from topic map[z]
				update(z, map[z], docs.get(z), 0);
				
				double[] probs = new double[K];
				double max = -Double.MAX_VALUE;
				for (int a = 0; a < K; a++) { //sense a
					double tmp = Math.log(alpha + d[a]);
					for (int j : fajs.get(z)) {
						int faj = fmj.get(z).get(j);
						for (int i = 0; i < faj; i++) {
							tmp += Math.log(beta + ekj[a][j] + i);
						}
					}
					double thing = beta * inverse.size() + ek[a];
					tmp += Gamma.logGamma(thing);
					tmp -= Gamma.logGamma(thing + docs.get(z).size());
					if (max < tmp) {
						max = tmp;
					}
					probs[a] = tmp;
				}
				double partition = 0;
				for (int a = 0; a < K; a++) {
					probs[a] = Math.exp(probs[a] - max);
					partition += probs[a];
				}
				normalize(probs, partition);
				int assign = flipACoin(r, probs);
				//add zth document to topic assign
				update(z, assign, docs.get(z), 1);
			}			
		}
	}
	
	public static int flipACoin(Random r, double[] dist) {
		double coin = r.nextDouble();
		double cumsum = 0;
		int assign = -1;
		for (int i = 0; i < dist.length; i++) {
			cumsum += dist[i];
			if (cumsum > coin) {
				assign = i;
				break;
			}
		}	
		return assign;
	}
	
	public static void normalize(double[] dist, double z) {
		for (int i = 0; i < dist.length; i++) dist[i] /= z;
	}
	
	public void printTopWords() {
		for (int j = 0; j < clusters.size(); j++) {
			List<Map.Entry<String, Double>> sorted = Sort.sortByValue(clusters.get(j));
			System.out.println("cluster: " + j);
			for (int k = 0; k < 10; k++) {
				if (k != 9) {
					System.out.print(sorted.get(k) + " ");
				} else {
					System.out.println(sorted.get(k));
				}
			}
		}
	}
	
	/* check whether distributions are indeed distributions */
	public void sanityCheck() {
		for (int i = 0; i < K; i++) {
			HashMap<String, Double> clu = clusters.get(i);
			double sanity = 0;
			double denom = total[i];
			for (Double val : clu.values()) {
				sanity += (val + beta) / (denom + (beta * (J+1)));
			}
			sanity += beta / (denom + (beta * (J+1))) * (J+1 - clu.size());
			System.out.println(sanity);
		}
	}
	
	public List<Integer> senseTags() {
		List<Integer> tags = new ArrayList<Integer>();
		for (int i = 0; i < map.length; i++) {
			int max = 0;
			int argmax = -1;
			for (int j = 0; j < K; j++) {
				if (topics[i][j] > max) {
					max = topics[i][j];
					argmax = j;
				}
			}	
			tags.add(argmax);
		}			
		return tags;
	}	
	
	public void update(int n, int k, List<Integer> line, int flag) {				
		if (flag == 0) { // remove document n from topic k
			map[n] = -1;
			d[k]--;				
			for (int j : line) ekj[k][j]--;					
			ek[k] -= line.size();
		} else { // add document n to topic k
			map[n] = k;
			d[k]++;
			for (int j : line) ekj[k][j]++;
			ek[k] += line.size();
		}
	}
}
