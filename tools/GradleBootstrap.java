import java.io.*;
import java.net.URI;
import java.net.http.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.zip.*;

/** Readable bootstrap, NOT an official Gradle wrapper JAR. Downloads only the published
 * Gradle distribution, validates its pinned official checksum, and extracts safely.
 * Run `./gradlew wrapper` once to replace the launchers with the official wrapper.
 */
public final class GradleBootstrap {
    static final String VERSION="8.13";
    static final String SHA256="20f1b1176237254a6fc204d8434196fa11a4cfb387567519c61556e8710aed78";
    public static void main(String[] args)throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("Pass the project root directory");
        Path root=Path.of(args[0]).toAbsolutePath().normalize();
        Path cache=root.resolve(".local-tools");Files.createDirectories(cache);
        try(var channel=FileChannel.open(cache.resolve("bootstrap.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE);
            var lock=channel.lock()) {
            Path home=cache.resolve("gradle-"+VERSION), marker=home.resolve(".nightkeys-verified");
            if(Files.isRegularFile(marker) && Files.readString(marker).trim().equals(SHA256)
                && Files.isRegularFile(home.resolve("bin/gradle")))return;
            Path archive=cache.resolve("gradle-"+VERSION+"-bin.zip");
            if(!Files.isRegularFile(archive)||!digest(archive).equals(SHA256)) {
                System.out.println("Downloading Gradle "+VERSION+"; the archive is verified against Gradle's published SHA-256.");
                URI url=URI.create("https://services.gradle.org/distributions/gradle-"+VERSION+"-bin.zip");
                var client=HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(20)).build();
                Path part=cache.resolve("gradle.part");Files.deleteIfExists(part);
                var request=HttpRequest.newBuilder(url).timeout(Duration.ofMinutes(10)).GET().build();
                var response=client.send(request,HttpResponse.BodyHandlers.ofFile(part));
                if(response.statusCode()!=200){Files.deleteIfExists(part);throw new IOException("Gradle download returned HTTP "+response.statusCode());}
                if(!digest(part).equals(SHA256)){Files.deleteIfExists(part);throw new SecurityException("Gradle checksum mismatch; refusing to execute it");}
                Files.move(part,archive,StandardCopyOption.REPLACE_EXISTING);
            }
            try(var zip=new ZipInputStream(Files.newInputStream(archive))){
                ZipEntry entry;long total=0;
                while((entry=zip.getNextEntry())!=null){
                    Path target=cache.resolve(entry.getName()).normalize();
                    if(!target.startsWith(home))throw new SecurityException("Unexpected ZIP path: "+entry.getName());
                    if(entry.isDirectory()){Files.createDirectories(target);continue;}
                    Files.createDirectories(target.getParent());
                    try(var out=Files.newOutputStream(target)){
                        byte[] buffer=new byte[65536];int count;
                        while((count=zip.read(buffer))!=-1){total+=count;if(total>1024L*1024*1024)throw new IOException("Unreasonable archive size");out.write(buffer,0,count);}
                    }
                }
            }
            home.resolve("bin/gradle").toFile().setExecutable(true,false);
            Files.writeString(marker,SHA256+"\n");
        }
    }
    private static String digest(Path file)throws Exception {
        var digest=MessageDigest.getInstance("SHA-256");
        try(var in=Files.newInputStream(file)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);}
        return HexFormat.of().formatHex(digest.digest());
    }
}
