import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.trees.PennTreebankTokenizer;
import edu.stanford.nlp.ie.machinereading.domains.ace.reader.*;

public class Misc {
	Document d;
	//List<Element> e;
	
	public Misc() {
		
	}
	
	public Document getDocument() {
		return d;
	}
	
	public List<Element> getList() {
		return d.getRootElement().elements();
	}
	
	public Misc lines(String source) {
		d = null;
		try {
			d = parse(source);
		}
		catch (DocumentException e) {
			System.err.println(e.getMessage());
		}
		return this;
	}
		
	public Document parse(String file) throws DocumentException {
		SAXReader reader = new SAXReader();
		Document document = reader.read(file);
		return document;
	}
	
	public void writeToFile(String file) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(file));
		for (Element ee : getList()) {
			bw.write(ee.getStringValue());
			bw.write("\n");
		}
		bw.flush();
		bw.close();
	}
	
	public static void main(String[] args) {
		//String test = "At the end of a long night of studying , I found myself in desperate need of a warm-from-the-oven chocolate chip cookie. Or , as it turns out , four. This recipe came to my rescue. Thank you .";
		//PennTreebankTokenizer tokenizer = new PennTreebankTokenizer(new StringReader(test));
		//RobustTokenizer tokenizer = new RobustTokenizer(test);
		//List<String> list = tokenizer.tokenize();
		//System.out.println(list);
		Misc m = new Misc();
		m.lines("/home/dc65/workspace/wsi/data/test_data/nouns/chip.n.xml");
		ArrayList<List<String>> d = new ArrayList<List<String>>();
		for (Element line : m.getList()) {
			PennTreebankTokenizer tokenizer = new PennTreebankTokenizer(new StringReader(line.getStringValue()));
			d.add(tokenizer.tokenize());
		}
		
		for (List<String> l : d) {
			System.out.println(l);
		}
	}
}
