package translator;

import parser.Parse;
import loader.Loader;



public class Translator {
	
	String classDir;
	
	Loader loader;
	Parse parser;
	
	public Translator(String dir) {
		this.classDir = dir;
		this.loader = new Loader(this.classDir);
		this.parser = new Parse(this.loader);
		this.parser.init();
	}
	
	public String go(String classFullName) throws ClassNotFoundException {
		return parser.parse(loader.load(classFullName));
	}
	
	
}
