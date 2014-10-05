import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class Sort {
	public static <A, B extends Number & Comparable<B>> ArrayList<Map.Entry<A, B>> 
	sortByValue(HashMap<A, B> t) {
		ArrayList<Map.Entry<A, B>> l = 
			new ArrayList<Map.Entry<A, B>>(t.entrySet());
		
		//sort in decreasing order
		Collections.sort(l, new Comparator<Map.Entry<A, B>>() {
			public int compare(Map.Entry<A, B> o1, Map.Entry<A, B> o2) {
				return o2.getValue().compareTo(o1.getValue());				
			}
		});
		return l;
	}	
}
