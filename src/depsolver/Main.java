package depsolver;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class Package {
  private String name;
  private String version;
  private Integer size;
  private List<List<String>> depends = new ArrayList<>();
  private List<String> conflicts = new ArrayList<>();

  public String getName() { return name; }
  public String getVersion() { return version; }
  public Integer getSize() { return size; }
  public List<List<String>> getDepends() { return depends; }
  public List<String> getConflicts() { return conflicts; }
  public void setName(String name) { this.name = name; }
  public void setVersion(String version) { this.version = version; }
  public void setSize(Integer size) { this.size = size; }
  public void setDepends(List<List<String>> depends) { this.depends = depends; }
  public void setConflicts(List<String> conflicts) { this.conflicts = conflicts; }
}

public class Main {
    private static List<Package> toInstall = new ArrayList<Package>();
    private static List<Package> toUninstall = new ArrayList<Package>();
    private static List<String> commands = new ArrayList<String>();;
  public static void main(String[] args) throws IOException {
    TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
    List<Package> repo = JSON.parseObject(readFile(args[0]), repoType);
    TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
    List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
    List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);

    //Debug Print
    /*for (Package p : repo) {
      System.out.printf("package %s version %s\n", p.getName(), p.getVersion());
      for (List<String> clause : p.getDepends()) {
        System.out.printf("  dep:");
        for (String q : clause) {
          System.out.printf(" %s", q);
        }
        System.out.printf("\n");
      }
    }
    //Print initial as strings
    System.out.printf("Initial: ");
    for (String i : initial) {
    	System.out.printf(" %s", i);
    }
    System.out.printf("\n");
    //Print positive constraints as strings
    System.out.printf("Positive constraints: ");
    for (String i : constraints) {
    	if(shouldInstall(i)) 
    	{
    		String[] split = i.split("\\+");
    		System.out.printf(" %s", split[1]);
    	}
    }
    System.out.printf("\n");
    //Print negative constraints as strings
    System.out.printf("Negative constraints: ");
    for (String i : constraints) {
    	if(!shouldInstall(i)) 
    	{
    		String[] split = i.split("\\-");
    		System.out.printf(" %s", split[1]);
    	}
    }
    System.out.printf("\n");
    */
    
    //Remove negative constraints from initial
    
    //Check positive constraints
    if (constraints != null) {
    	for (String i : constraints) {
        	if(shouldInstall(i)) 
        	{
        		String[] split = i.split("\\+");
        		String pack = split[1];
        		for (Package rep : repo) {
        			if (rep.getName().equals(pack)) {
        				//Install
        				install(rep, repo);
        			}
        		}
        	}
    	}
    }
    //Debug answer print
    /*    
    System.out.printf("final:\n");
    for (Package p : toInstall) {
        System.out.printf("package %s version %s\n", p.getName(), p.getVersion());
    }*/
    writeCommands(toInstall,toUninstall);
    String res = JSON.toJSONString(commands, true);
    System.out.println(commands);
  }

  static String readFile(String filename) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(filename));
    StringBuilder sb = new StringBuilder();
    br.lines().forEach(line -> sb.append(line));
    return sb.toString();
  }
  
  private static boolean shouldInstall(String s) {
	  if (s.contains("+")) {
		  return true;
	  }
	  else if (s.contains("-")) {
		  return false;
	  }
	  return false;
	  
  }
  
  private static void install (Package rep, List<Package> repo) {
		for (List<String> deps : rep.getDepends()) {
			List<Package> current = new ArrayList<Package>();
			//find smallest
			for (String q : deps) {
				Package add = dependencySearch(q,repo);
				if (add!=null) {
						current.add(add);
				}
			}
			if (!current.isEmpty()) {
			install(installSmallest(current), repo);
			}
		}
		boolean isInstalled = false;
		for (Package installed : toInstall) {
			if (installed.getName().equals(rep.getName()))
			{
				if (installed.getSize() > rep.getSize()) {
					toInstall.remove(installed);
					toInstall.add(rep);
				}
				isInstalled = true;
			}
		}
		if (!isInstalled) {
			toInstall.add(rep);
		}
  }
  
  private static Package dependencySearch(String q, List<Package> p) {
		if (q.contains(">=")) {
			String[] qSplit = q.split(">=");
			for (Package rep : p) {
				if (rep.getName().equals(qSplit[0]) && Double.parseDouble(rep.getVersion()) >= Double.parseDouble(qSplit[1])) {
					return rep;
				}
			}
		}
		if (q.contains("<=")) {
			String[] qSplit = q.split("<=");
			for (Package rep : p) {
				if (rep.getName().equals(qSplit[0]) && Double.parseDouble(rep.getVersion()) <= Double.parseDouble(qSplit[1])) {
					return rep;
				}
			}
		}
		if (q.contains("<")) {
			String[] qSplit = q.split("<");
			for (Package rep : p) {
				if (rep.getName().equals(qSplit[0]) && Double.parseDouble(rep.getVersion()) >= Double.parseDouble(qSplit[1])) {
					return rep;
				}
			}
		}
		if (q.contains(">")) {
			String[] qSplit = q.split(">");
			for (Package rep : p) {
				if (rep.getName().equals(qSplit[0]) && Double.parseDouble(rep.getVersion()) >= Double.parseDouble(qSplit[1])) {
					return rep;
				}
			}
		}
		if (q.contains("=")) {
			String[] qSplit = q.split("=");
			for (Package rep : p) {
				if (rep.getName().equals(qSplit[0]) && Double.parseDouble(rep.getVersion()) >= Double.parseDouble(qSplit[1])) {
					return rep;
				}
			}
		}
		else {
			String[] qSplit = {q,""};
			for (Package rep : p) {
				if (rep.getName().equals(qSplit[0])) {
					return rep;
				}
			}
		}
		return null;
  }
  
  private static Package installSmallest(List<Package> p) {
	  Package smallest = p.get(0);
	  int smallestSize = smallest.getSize();
	  if (p.size()>1) {
		  for (Package current : p) {
			  int size = current.getSize();
			  if (size<smallestSize) {
				  smallest = current;
			  }
		  }
	  }
	  return smallest;
  }
  
  private static void writeCommands (List<Package> install, List<Package> uninstall) {
	  for (Package installPack : install) {
		  commands.add("+" + installPack.getName() + "=" + installPack.getVersion());
	  }
	  for (Package uninstallPack : uninstall) {
		  commands.add("-" + uninstallPack.getName() + "=" + uninstallPack.getVersion());
	  }
  }
}
