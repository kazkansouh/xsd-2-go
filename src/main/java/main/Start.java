package main;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import translator.Translator;

public class Start {

	public void run_aux(Translator t, File folder, String pkg) throws ClassNotFoundException {
	    File[] files = folder.listFiles();
	    for (File f : files) {
		if (f.isDirectory()) {
		    if (pkg.equals("")) {
			run_aux(t, f, f.getName());
		    } else {
			run_aux(t, f, pkg + "." + f.getName());
		    }
		} else {
		    if (f.getName().endsWith(".class")) {
			String fname = f.getName().replaceAll("\\.class", "");
			String src = t.go(pkg + "." + fname);
			System.out.println(src);
		    }
		}
	    }
	}

	public void run(String folder) throws ClassNotFoundException {
		Translator t = new Translator(folder);
		File dir = new File(folder);

		run_aux(t, dir, "");
	}

	public static void main(String[] args) throws ClassNotFoundException, MalformedURLException {

		String classDir = args[0];
		
		Start s = new Start();
		s.run(classDir);
		
	}

}
