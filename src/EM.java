import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class EM {
	List<List<String>> docs;
	List<String> senses;
	HashMap<String, Double> langMod;
	List<HashMap<String, Double>> clusters; 
	List<Double> td;
	List<List<Integer>> dist;
	
	HashMap<String, Double> sigma;
	HashMap<Pair<String, String>, Double> tau;
	HashMap<String, Double> exp1; 
	HashMap<Pair<String, String>, Double> exp2;	
	
	double beta;;
	double power;
	
	int size = 0;
	
	public EM(List<List<String>> docs, List<String> senses,
			HashMap<String, Double> langMod, List<HashMap<String, Double>> clusters, 
      List<Double> td, List<List<Integer>> dist, double power, double beta) {
		this.docs = docs;
		this.senses = senses;
		this.langMod = langMod;
		this.clusters = clusters;
		this.td = td;
		this.dist = dist;
		this.power = power;
    this.beta = beta;
				
		for (List<Integer> ddist : dist) {
			for (int i : ddist) {
				if (i > size) size = i;
			}
		}
		
		sigma = new HashMap<String, Double>();
		tau = new HashMap<Pair<String, String>, Double>();
		exp1 = new HashMap<String, Double>(); 
		exp2 = new HashMap<Pair<String, String>, Double>();
	}
	
	public double e() {
		ArrayList<HashMap<Pair<String, String>, Double>> normalized 
			= new ArrayList<HashMap<Pair<String, String>, Double>>();
		
		normalized.add(tau);
		for (int i = 2; i <= size; i++) {
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
		
		double p = 0;
		// compute log-likelihood of data
		for (int i = 0; i < docs.size(); i++) {
			List<String> line = docs.get(i);
			List<Integer> dList = dist.get(i);
			if (line.isEmpty()) continue;
			double tmp = 0; 
			for (String s : senses) {
				double tmp2 = sigma.get(s);
				for (int j = 0; j < line.size(); j++) {
					String w = line.get(j);
					Pair<String, String> key = new Pair<String, String>(s, w);
					double val = normalized.get(dList.get(j)-1).get(key);
					tmp2 *= val / langMod.get(w);
				}
				tmp += tmp2;
			}			
			if (!Double.isInfinite(Math.log(tmp))) {
				p += Math.log(tmp);
			}
		}
		
		for (Pair<String, String> key : exp2.keySet()) {
			exp2.put(key, 0d);			
		}
		for (String key : exp1.keySet()) {
			exp1.put(key, 0d);
		}
		
		for (int j = 0; j < docs.size(); j++) {
			List<String> line = docs.get(j);
			List<Integer> dList = dist.get(j);
			if (line.isEmpty()) continue;
			double z = 0;
			List<Double> probs = new ArrayList<Double>();
			List<Double> probs2 = new ArrayList<Double>();
			double max = -Double.MAX_VALUE;
			for (String s : senses) {
				double tmp = Math.log(sigma.get(s));
				//double tmp = 0;
				for (int i = 0; i < line.size(); i++) {
					Pair<String, String> key = new Pair<String, String>(s, line.get(i));
					double val = normalized.get(dList.get(i)-1).get(key);
					tmp += Math.log(val);
				}
				probs2.add(tmp);
				if (tmp > max) max = tmp;
			}
			for (double pp : probs2) {
				double tmp = Math.exp(pp - max);
				probs.add(tmp);
				z += tmp;
			}
				
			for (int i = 0; i < senses.size(); i++) {
				for (String w : line) {
					Pair<String, String> key = new Pair<String, String>(senses.get(i),w);
					exp2.put(key, exp2.get(key) + probs.get(i) / z);
				}
				exp1.put(senses.get(i), exp1.get(senses.get(i)) + probs.get(i) / z);
			}
		}		
		return p;
	}
	
	public HashMap<String, Double> getSigma() {
		return sigma;
	}
	
	public HashMap<Pair<String, String>, Double> getTau() {
		return tau;
	}
	
	public void init() {
		double[] total = new double[senses.size()];
		for (int i = 0; i < senses.size(); i++) {
			double val = 0;
			for (Double dd : clusters.get(i).values()) val += dd;
			total[i] = val;
		}
		
		int J = langMod.size();
		
		for (int i = 0; i < senses.size(); i++) {
			String sense = senses.get(i);
			HashMap<String, Double> clu = clusters.get(i);
			for (List<String> line : docs) {
				for (String w : line) {
					Pair<String, String> key = new Pair<String, String>(sense, w);
					if (!tau.containsKey(key)) {
						double val;
						if (clu.containsKey(w)) {
							val = (clu.get(w) + beta) / (total[i] + beta * J);
						} else {
							val = beta / (total[i] + beta * J);
						}						
						tau.put(key, val);
						exp2.put(key, 0d);
					}
				}
			}
			sigma.put(sense, td.get(i));
			exp1.put(sense, 0d);
		}
	}
	
	public void m() {
		HashMap<String, Double> tmp = new HashMap<String, Double>();
		for (String s : senses) {
			tmp.put(s, 0d);
		}
		for (Pair<String, String> key : exp2.keySet()) {
			tmp.put(key.getFirst(), tmp.get(key.getFirst()) + exp2.get(key));
		}		
		for (Pair<String, String> key : tau.keySet()) {
			tau.put(key, exp2.get(key) / tmp.get(key.getFirst())); 
		}

		double z = 0;
		for (Double d : exp1.values()) {
			z += d;
		}
		
		for (String key : sigma.keySet()) {
			sigma.put(key, exp1.get(key) / z);
		}
	}	
	
	public void run() {
		init();
		double prev = 0;
		for (int i = 0; i < 100; i++) {
			double p = e();
			System.out.println(i + ": " + p);
			if (Math.abs(prev - p) / Math.abs(p) < 0.01) break;
			m();
			prev = p;
		}
	}	
}
