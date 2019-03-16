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
    private static List<String> commands = new ArrayList<String>();
    private static List<Package> ignore = new ArrayList<Package>();
    private static List<Package> initialPacks = new ArrayList<Package>();
  public static void main(String[] args) throws IOException {
    TypeReference<List<Package>> repoType = new TypeReference<List<Package>>() {};
    List<Package> repo = JSON.parseObject(readFile(args[0]), repoType);
    TypeReference<List<String>> strListType = new TypeReference<List<String>>() {};
    List<String> initial = JSON.parseObject(readFile(args[1]), strListType);
    List<String> constraints = JSON.parseObject(readFile(args[2]), strListType);
    
    //Define cost
    long cost = 0;
    
    initialPacks(repo, initial);
    //Check positive and negative constraints
    if (constraints != null) {
    	for (String i : constraints) {
        	if(shouldInstall(i)) {
        		String[] split = i.split("\\+");
        		String constraint = split[1];
        		for (Package rep : repo) {
        			if (rep.getName().equals(constraint)) {
        				install(rep, repo, constraint, ignore, initial);
        			}
        		}
        	}
        	else {
        		String[] split = i.split("\\-");
        		String constraint = split[1];
        		for (Package rep : repo) {
        			if (rep.getName().equals(constraint)) {
        				uninstall(rep, repo);
        			}
        		}
        	}
    	}
    }
    writeCommands(toInstall,toUninstall);
    String res = JSON.toJSONString(commands, true);
    System.out.println(commands);
    for (Package installed : toInstall) {
    	cost = cost + installed.getSize();
    }
    for (Package installed : toUninstall) {
    	cost = cost + 1000000;
    }
    //System.out.println("Cost:" + cost);
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
  
  private static void install (Package rep, List<Package> repo, String constraint, List<Package> ignore, List<String> initial) {
	  	boolean isInstalled = false;
	  	List<Package> current = new ArrayList<Package>();
	  	if (!rep.getConflicts().isEmpty()) {	
	  		for (String conflicts : rep.getConflicts())
	  			checkConflicts(conflicts, repo, initial);
	  	}
		for (List<String> deps : rep.getDepends()) {
			for (String q : deps) {
				if (!q.equals(constraint)) {
					Package add = dependencySearch(q,repo);
					if (add!=null) {
							current.add(add);
					}
				}
				else {
					for (Package loopPack : repo) {
						if (loopPack.getName().equals(q)) {
							for (List<String> loopDeps : loopPack.getDepends()) {
								for (String loopDep : loopDeps) {
									Package add = dependencySearch(loopDep,repo);
									if (add!=null) {
											current.add(add);
									}
								}
							}
							Package pack = (getSmallest(current, ignore));
							ignore.add(pack);
							install(loopPack, repo, constraint, ignore, initial);
							}
						}
					}
			}
			if (!current.isEmpty()) {
				install(getSmallest(current, ignore), repo, constraint, ignore, initial);
			}
		}	
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
    		for (Package loopRep : repo) {
    			if (loopRep.getName().equals(constraint)) {
    				if (!toInstall.contains(loopRep)) {
    					toInstall.add(rep);
    					break;
    				}
    			}
    		}
		}
  }
  
  private static void uninstall (Package rep, List<Package> repo) {
	  toUninstall.add(rep);
  }
  
  private static void initialPacks (List<Package> repo, List<String> initial) {
	  for (String packs : initial) {
			String[] qSplit = packs.split("=");
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0])) {
					if (compareVersions(rep.getVersion(),qSplit[1],"=")) {
						initialPacks.add(rep);
					}
				}
			}
	  }
  }
  
  private static Package dependencySearch(String q, List<Package> repo) {
		if (q.contains(">=")) {
			String[] qSplit = q.split(">=");
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0]) && Double.parseDouble(rep.getVersion()) >= Double.parseDouble(qSplit[1])) {
					return rep;
				}
			}
		}
		if (q.contains("<=")) {
			String[] qSplit = q.split("<=");
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0]) && Double.parseDouble(rep.getVersion()) <= Double.parseDouble(qSplit[1])) {
					return rep;
				}
			}
		}
		if (q.contains("<") && !q.contains("<=")) {
			String[] qSplit = q.split("<");
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0]) && Double.parseDouble(rep.getVersion()) < Double.parseDouble(qSplit[1])) {
					return rep;
				}
			}
		}
		if (q.contains(">") && !q.contains(">=")) {
			String[] qSplit = q.split(">");
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0]) && Double.parseDouble(rep.getVersion()) > Double.parseDouble(qSplit[1])) {
					return rep;
				}
			}
		}
		if (q.contains("=")) {
			String[] qSplit = q.split("=");
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0]) && Double.parseDouble(rep.getVersion()) == Double.parseDouble(qSplit[1])) {
					return rep;
				}
			}
		}
		else {
			String[] qSplit = {q,""};
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0])) {
					return rep;
				}
			}
		}
		return null;
  }
  
  private static Package checkConflicts(String q, List<Package> repo, List<String> initial) {
		if (q.contains(">=")) {
			String[] qSplit = q.split(">=");
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0])) {
					if (compareVersions(rep.getVersion(),qSplit[1],"<=")) {
						if (initialPacks.contains(rep)) {
							uninstall(rep, repo);
						}
					}
				}
			}
		}
		if (q.contains("<=")) {
			String[] qSplit = q.split("<=");
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0])) {
					if (compareVersions(rep.getVersion(),qSplit[1], "<=")) {
						if (initialPacks.contains(rep)) {
							uninstall(rep, repo);
						}
					}
				}
			}
		}
		if (q.contains("<") && !q.contains("<=")) {
			String[] qSplit = q.split("<");
			System.out.println(q);
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0])) {
					if (compareVersions(rep.getVersion(),qSplit[1],"<")) {
						if (initialPacks.contains(rep)) {
							uninstall(rep, repo);
						}
					}
				}
			}
		}
		if (q.contains(">") && !q.contains(">=")) {
			String[] qSplit = q.split(">");
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0])) {
					if (compareVersions(rep.getVersion(),qSplit[1],">")) {
						if (initialPacks.contains(rep)) {
							uninstall(rep, repo);
						}
					}
				}
			}
		}
		if (q.contains("=")) {
			String[] qSplit = q.split("=");
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0])) {
					if (compareVersions(rep.getVersion(),qSplit[1],"=")) {
						if (initialPacks.contains(rep)) {
							uninstall(rep, repo);
						}
					}
				}
			}
		}
		else {
			String[] qSplit = {q,""};
			for (Package rep : repo) {
				if (rep.getName().equals(qSplit[0])) {
					if (initialPacks.contains(rep)) {
						uninstall(rep, repo);
					}
				}
			}
		}
		return null;
  }
  
  private static boolean compareVersions(String rep, String q, String sign) {
	  boolean b = false;
	  boolean also = false;
	  String[] s1 = rep.split("\\.");
	  String[] s2 = q.split("\\.");
	  if (sign.equals(">=")) {
		  for (int pos = 0; pos < s1.length; pos++) {
			    if (Integer.parseInt(s1[pos]) >= Integer.parseInt(s2[pos])) {
			        also = true;
			    }
			    else {
			    	if (!also) {
			    		b = false;
			    	}
			    	else {
			    		b = true;
			    	}
			    }
			}
	  }
	  if (sign.equals("<=")) {
		  for (int pos = 0; pos < s1.length; pos++) {
			    if (Integer.parseInt(s1[pos]) <= Integer.parseInt(s2[pos])) {
			        b = true;
			    } else {
			        b= false;
			        break;
			    }
		  }
	  }
	  if (sign.equals(">")) {
		  for (int pos = 0; pos < s1.length; pos++) {
			    if (Integer.parseInt(s1[pos]) >= Integer.parseInt(s2[pos])) {
			        b = true;
			        break;
			    }
			    else {
			    	b = false;
			    }
			}
	  }
	  if (sign.equals("<")) {
		  for (int pos = 0; pos < s1.length; pos++) {
			    if (Integer.parseInt(s1[pos]) < Integer.parseInt(s2[pos])) {
			        b = true;
			        break;
			    } else {
			        b= false;
			    }
		  }
	  }
	  if (sign.equals("=")) {
		  for (int pos = 0; pos < s1.length; pos++) {
			    if (Integer.parseInt(s1[pos]) == Integer.parseInt(s2[pos])) {
			        b = true;
			    } else {
			        b= false;
			    }
			}
	  }
	  return b;
	  
  }
  
  private static Package getSmallest(List<Package> p, List<Package> ignore) {
	  Package smallest = null;
	  for (Package pack : p) {
		  if (!ignore.contains(pack)) {
			  smallest = pack;
			  break;
		  }
	  }
	  int smallestSize = smallest.getSize();
	  if (p.size()>1) {
		  for (Package current : p) {
			  if (!ignore.contains(current)) {
				  int size = current.getSize();
				  if (size<smallestSize) {
					  smallest = current;
				  }
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
