package com.rmaj91;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.function.Function;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {

        String[] dirNames = {"HOME", "DEV", "TEST"};
        Arrays.stream(dirNames).forEach(dir -> new File(dir).mkdir());

        Files.createFile(Path.of("HOME\\count.txt"));
        WatchService watchService = FileSystems.getDefault().newWatchService();
        Path path = Paths.get("HOME");

        path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        WatchKey key;

        int movedToDev = 0;
        int movedToTest = 0;

        while ((key = watchService.take()) != null) {
            for (WatchEvent<?> event : key.pollEvents()) {
                File fileToMove = new File("home\\" + event.context().toString());
                Path pathToMoveFile = null;
                if (event.context().toString().endsWith(".xml")) {
                    pathToMoveFile = Path.of("DEV\\" + event.context());
                    movedToDev++;
                } else if (event.context().toString().endsWith(".jar")) {
                    Date date = getDate(fileToMove);
                    if (date.getHours() % 2 == 0) {
                        pathToMoveFile = Path.of("DEV\\" + event.context());
                        movedToDev++;
                    } else {
                        pathToMoveFile = Path.of("TEST\\" + event.context());
                        movedToTest++;
                    }
                } else {
                    continue;
                }
                Optional.ofNullable(pathToMoveFile)
                        .map(moveFile(fileToMove));

                String format = String.format("Moved to DEV: %d \nMoved to TEST: %d \nMoved total: %d",
                        movedToDev, movedToTest, movedToDev + movedToTest);
                Files.write(Path.of("HOME\\count.txt"), format.getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            }
            key.reset();
        }
    }

    private static Function<Path, Path> moveFile(File fileToMove) {
        return pathToMove -> {
            try {
                return Files.move(fileToMove.toPath(), pathToMove);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        };
    }

    private static Date getDate(File fileToMove) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(Path.of(fileToMove.toURI()), BasicFileAttributes.class);
        FileTime fileTime = attributes.creationTime();
        return new Date(fileTime.toMillis());
    }
}

