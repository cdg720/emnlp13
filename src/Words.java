import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashSet;

public class Words {
	ArrayList<String> list;
	
	public Words(String file) {
		list = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new FileReader(file));
			String line = br.readLine();
			while (line != null) {
				list.add(line.trim());
				line = br.readLine();
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
		}
	}
	
	public HashSet<String> getSet() {
		return new HashSet<String>(list);
	}
	
	public ArrayList<String> getList() {
		return list;
	}
}
