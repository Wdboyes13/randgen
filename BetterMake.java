import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.*;

import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

    public class BetterMake {
        public static List<Path> findCFiles(String rootDir) throws IOException {
        return Files.walk(Paths.get(rootDir))
                .filter(p -> p.toString().endsWith(".c"))
                .collect(Collectors.toList());
    }
    public static List<String> compileCFiles(List<Path> cFiles, String buildDir, String CC, String flags) throws IOException, InterruptedException {
        List<String> objectFiles = new ArrayList<>();
        Files.createDirectories(Paths.get(buildDir));

        for (Path cFile : cFiles) {
            String cFileName = cFile.getFileName().toString();
            String objName = cFileName.replace(".c", ".o");
            Path objPath = Paths.get(buildDir, objName);

            List<String> cmd = new ArrayList<>(List.of(CC.split(" ")));
            cmd.add("-c");
            cmd.add(cFile.toString()); 
            cmd.add("-o");
            cmd.add(objPath.toString());
            cmd.addAll(List.of(flags.split(" ")));
            System.out.println("Compiling: " + String.join(" ", cmd));
            Process p = new ProcessBuilder(cmd).inheritIO().start();
            int exit = p.waitFor();
            if (exit != 0) throw new RuntimeException("Compilation failed for: " + cFile);
            objectFiles.add(objPath.toString());
        }
        return objectFiles;
    }
        public static void linkObjects(List<String> objectFiles, String outputBinary, String CC, String flags) throws IOException, InterruptedException {
        List<String> cmd = new ArrayList<>(List.of(CC.split(" ")));
        cmd.addAll(objectFiles);
        cmd.add("-o");
        cmd.add(outputBinary);
        cmd.addAll(List.of(flags.split(" ")));

        System.out.println("Linking: " + String.join(" ", cmd));
        Process p = new ProcessBuilder(cmd).inheritIO().start();
        int exit = p.waitFor();
        if (exit != 0) throw new RuntimeException("Linking failed.");
    }
        private static String getTagContent(Element parent, String childTag, String tag) {
        NodeList childNodes = parent.getElementsByTagName(childTag);
        if (childNodes.getLength() > 0) {
            Element childElement = (Element) childNodes.item(0);
            NodeList tagList = childElement.getElementsByTagName(tag);
            if (tagList.getLength() > 0) {
                return tagList.item(0).getTextContent();
            }
        }
        return null;  // Return null if tag is not found
    }
    public static HashMap<String, String> parse(){
        HashMap<String, String> data = new HashMap<>();
        try {
            File inputFile = new File("mk.xml");  // Your XML filename here

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(inputFile);
            doc.getDocumentElement().normalize();
            NodeList CCs = doc.getElementsByTagName("Compilers");
             NodeList SRC = doc.getElementsByTagName("SRC");
             NodeList GIT = doc.getElementsByTagName("GIT");

// Ensure the Compilers node exists
            if (CCs.getLength() > 0) {
                Element compilersElement = (Element) CCs.item(0);

                // Now safely try to get ARM and SF nodes under Linux and Mac
                String linuxArm = getTagContent(compilersElement, "Linux", "ARM");
                String linux64 = getTagContent(compilersElement, "Linux", "SF");
                String macArm = getTagContent(compilersElement, "Mac", "ARM");
                String mac64 = getTagContent(compilersElement, "Mac", "SF");
                String winArm = getTagContent(compilersElement, "Win", "ARM");
                String win64 = getTagContent(compilersElement, "Win", "SF");
                System.out.println("Linux ARM: " + linuxArm);
                System.out.println("Linux 64: " + linux64);
                System.out.println("Mac ARM: " + macArm);
                System.out.println("Mac 64: " + mac64);
                System.out.println("Win ARM: " + winArm);
                System.out.println("Win 64: " + win64);

                data.put("LINARMCC", linuxArm);
                data.put("LIN64CC", linux64);
                data.put("MACARMCC", macArm);
                data.put("MAC64CC", mac64);
                data.put("WIN64CC", win64);
                data.put("WINARMCC", winArm);

                String globLinks = getTagContent(compilersElement, "flags", "globalLink");
                String globComps = getTagContent(compilersElement, "flags", "globalComp");
                System.out.println("Global Linker Flags: " + globLinks);
                data.put("GLOBFLAGSL", globLinks);
                System.out.println("Global Compiler Flags: " + globComps);
                data.put("GLOBFLAGS", globComps);
            } else {System.out.println("Compiler Tags Invalid"); System.exit(1);}

            String fileName = doc.getElementsByTagName("FILE").item(0).getTextContent(); // 2nd <FILE>
            if (SRC.getLength() > 0){
                String srcType = doc.getElementsByTagName("Type").item(0).getTextContent();
                String srcFile = doc.getElementsByTagName("FILE").item(1).getTextContent(); // 3rd <FILE>
                System.out.println("Source Type: " + srcType);
                System.out.println("Source File: " + srcFile);
                data.put("SRCT", srcType);
                data.put("SRCF", srcFile);
            } else {System.out.println("Project Data Tags Invalid"); System.exit(1);}
            if (GIT.getLength() > 0){
                String repo = doc.getElementsByTagName("REPO").item(0).getTextContent();
                String msg = doc.getElementsByTagName("COM-MSG").item(0).getTextContent();
                System.out.println("Git Repo: " + repo);
                System.out.println("Git Commit Message: " + msg);
                data.put("REPO", repo);
                data.put("MSG", msg);
            }
            System.out.println("Main Output File: " + fileName);

            data.put("OUTF", fileName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }
    public static void gitUpdate(HashMap<String, String> data ) throws IOException, InterruptedException{
        String msg = new String();
        if (data.get("REPO")!=null && !data.get("REPO").isEmpty()) {
            String repo = data.get("REPO");
            if (data.get("MSG")!=null && !data.get("MSG").isEmpty()) msg=data.get("MSG"); else msg="Updated";
            if (msg.equals("PROMPT")){
                Scanner inp = new Scanner(System.in);
                msg = inp.nextLine();
            }
            String[] cmd = {"bash", "-c", "git add . && git commit -m \"" + msg + "\" && git push " + repo + " main"};
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.inheritIO();
            Process proc = pb.start();
            proc.waitFor();
        }
    }
    public static void MFrunCC(String CC, String SRCDir, String OUTF, String PLATFORM, String GLOBFLAGSL, String GLOBFLAGS){
        try {
        List<Path> cFiles = findCFiles(SRCDir);
        List<String> objectFiles = compileCFiles(cFiles, "build/"+PLATFORM, CC, GLOBFLAGS);
        linkObjects(objectFiles, OUTF, CC, GLOBFLAGSL);

        System.out.println("Build complete! Output: " + OUTF);
        } catch (IOException | InterruptedException e){
            System.out.println("Build Failed");
            e.printStackTrace();
        }
    }
    public static void OFrunCC(String CC, String SRC, String OUTF, String FLAGS){
        try {
        List<String> cmdList = new ArrayList<>(List.of(CC.split(" ")));
        cmdList.add(SRC);
        cmdList.add("-o");
        cmdList.add(OUTF);
        cmdList.addAll(List.of(FLAGS.split(" ")));
        System.out.println("Running: " + String.join(" ", cmdList));
        ProcessBuilder CCPB = new ProcessBuilder(cmdList);
        Process CCProc = CCPB.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(CCProc.getErrorStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println("[ERROR] " + line);
            }
        }
        CCProc.waitFor();

        System.out.println("CC: " + CC + " PID: " + CCProc.pid() + " EXIT CODE: " + CCProc.exitValue());
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }
    }
    
    public static void OF(HashMap<String, String> data) throws IOException, InterruptedException{
        Thread LINARM = new Thread(() ->{
            if (data.get("LINARMCC")!=null && !data.get("LINARMCC").isEmpty()) OFrunCC(data.get("LINARMCC"), data.get("SRCF"), "rls/linARM/"+data.get("OUTF"), data.get("GLOBFLAGSL") + " " + data.get("GLOBFLAGS"));
        });
        Thread LIN = new Thread(() ->{
            if (data.get("LIN64CC")!=null && !data.get("LIN64CC").isEmpty()) OFrunCC(data.get("LIN64CC"), data.get("SRCF"), "rls/lin/"+data.get("OUTF"), data.get("GLOBFLAGSL") + " " + data.get("GLOBFLAGS"));
        });
        Thread MACARM = new Thread(() ->{
            if (data.get("MACARMCC")!=null && !data.get("MACARMCC").isEmpty()) OFrunCC(data.get("MACARMCC"), data.get("SRCF"), "rls/macARM/"+data.get("OUTF"), data.get("GLOBFLAGSL") + " " + data.get("GLOBFLAGS"));
        });
        Thread MAC = new Thread(() ->{
            if (data.get("MAC64CC")!=null && !data.get("MAC64CC").isEmpty())OFrunCC(data.get("MAC64CC"), data.get("SRCF"), "rls/mac/"+data.get("OUTF"), data.get("GLOBFLAGSL") + " " + data.get("GLOBFLAGS"));
        });
        Thread WIN = new Thread(() ->{
            if (data.get("WIN64CC")!=null && !data.get("WIN64CC").isEmpty())OFrunCC(data.get("WIN64CC"), data.get("SRCF"), "rls/win/"+data.get("OUTF"), data.get("GLOBFLAGSL") + " " + data.get("GLOBFLAGS"));
        });
        Thread WINARM = new Thread(() ->{
            if (data.get("WINARMCC")!=null && !data.get("WINARMCC").isEmpty())OFrunCC(data.get("WINARMCC"), data.get("SRCF"), "rls/winARM/"+data.get("OUTF"), data.get("GLOBFLAGSL") + " " + data.get("GLOBFLAGS"));
        });
        LINARM.start();
        LIN.start();
        MACARM.start();
        MAC.start();
        WIN.start();
        WINARM.start();

        LINARM.join();
        LIN.join();
        MACARM.join();
        MAC.join();
        WIN.join();
        WINARM.join();
    }
    
    public static void MF(HashMap<String, String> data) throws IOException, InterruptedException{
        Thread LINARM = new Thread(() ->{
            if (data.get("LINARMCC")!=null && !data.get("LINARMCC").isEmpty()) MFrunCC(data.get("LINARMCC"), data.get("SRCF"), "rls/linARM/"+data.get("OUTF"), "LINARM", data.get("GLOBFLAGSL"), data.get("GLOBFLAGS"));
        });
        Thread LIN = new Thread(() ->{
            if (data.get("LIN64CC")!=null && !data.get("LIN64CC").isEmpty()) MFrunCC(data.get("LIN64CC"), data.get("SRCF"), "rls/lin/"+data.get("OUTF"), "LIN64", data.get("GLOBFLAGSL"), data.get("GLOBFLAGS"));
        });
        Thread MACARM = new Thread(() ->{
            if (data.get("MACARMCC")!=null && !data.get("MACARMCC").isEmpty()) MFrunCC(data.get("MACARMCC"), data.get("SRCF"), "rls/macARM/"+data.get("OUTF"), "MACARM", data.get("GLOBFLAGSL"), data.get("GLOBFLAGS"));
        });
        Thread MAC = new Thread(() ->{
            if (data.get("MAC64CC")!=null && !data.get("MAC64CC").isEmpty())MFrunCC(data.get("MAC64CC"), data.get("SRCF"), "rls/mac/"+data.get("OUTF"), "MAC64", data.get("GLOBFLAGSL"), data.get("GLOBFLAGS"));
        });
        Thread WIN = new Thread(() ->{
            if (data.get("WIN64CC")!=null && !data.get("WIN64CC").isEmpty())MFrunCC(data.get("WIN64CC"), data.get("SRCF"), "rls/win/"+data.get("OUTF"), "WIN64", data.get("GLOBFLAGSL"), data.get("GLOBFLAGS"));
        });
        Thread WINARM = new Thread(() ->{
            if (data.get("WINARMCC")!=null && !data.get("WINARMCC").isEmpty())MFrunCC(data.get("WINARMCC"), data.get("SRCF"), "rls/winARM/"+data.get("OUTF"), "WINARM", data.get("GLOBFLAGSL"), data.get("GLOBFLAGS"));
        });
        LINARM.start();
        LIN.start();
        MACARM.start();
        MAC.start();
        WIN.start();
        WINARM.start();

        LINARM.join();
        LIN.join();
        MACARM.join();
        MAC.join();
        WIN.join();
        WINARM.join();
    }
    public static void main(String[] args) throws InterruptedException, IOException{
        HashMap<String, String> data = parse();
        String SRCT = data.get("SRCT");
        if (SRCT.equals("OneFile")) OF(data);
        if (SRCT.equals("MultiFile")) MF(data);
        System.out.println(("Running git"));
        gitUpdate(data);
    }
}