package org.flussig.documentation;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.flussig.documentation.text.Anchor;
import org.flussig.documentation.text.Reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Compile goal:
 * searches through README.md files in source directory and subdirectories,
 * replaces placeholders for DACDOC tests with green/red/orange/grey pics
 */
@Mojo(name = "compile")
public class DacDocCompile
    extends AbstractMojo
{
    @Parameter(readonly = true, defaultValue = "${project.basedir}")
    private File srcDirectory;

    public void execute() throws MojoExecutionException
    {
        try {
            File allSourceDir = srcDirectory;

            getLog().info( String.format("Build directory: %s", allSourceDir.getAbsolutePath()));

            // prepare source directory: create resource folder with images for check results (if not exists)
            prepareResourceDirectory(allSourceDir);

            // collect all readme files
            Set<File> readmeFiles = Reader.findMarkdownFiles(allSourceDir.toPath());

            getLog().info( String.format("Readme files: %s", readmeFiles));

            // parse and find all placeholders
            Map<File, Set<Anchor>> parsedAnchors = Reader.parseFiles(readmeFiles);

            // replace DACDOC placeholders with indicators of check results
            Path dacdocResources = Path.of(allSourceDir.getAbsolutePath(), Constants.DACDOC_RESOURCES);
            getLog().info( String.format("DacDoc resource directory: %s", dacdocResources));

            Map<File, String> processedFiles = Reader.getTransformedFiles(parsedAnchors, dacdocResources);

            // add indicators of check results to each readme file
            for(var fileContent: processedFiles.entrySet()) {
                Files.writeString(fileContent.getKey().toPath(), fileContent.getValue());
            }
        } catch(Exception e) {
            throw new MojoExecutionException("exception while executing dacdoc-maven-plugin compile goal " + e.getMessage());
        }
    }

    // write necessary resources to dacdoc-resources directory
    private void prepareResourceDirectory(File baseDir) throws IOException {
        File destDacDocResourceDirectory = createDacDocResourceDir(baseDir);

        List<String> indicatorFileNames = Arrays.asList(Constants.GREY_IND, Constants.GREEN_IND, Constants.ORANGE_IND, Constants.RED_IND);

        for(String indicatorFileName: indicatorFileNames) {
            Path outPath = Path.of(destDacDocResourceDirectory.getAbsolutePath(), indicatorFileName);

            try(InputStream stream = getClass().getClassLoader().getResource(indicatorFileName).openStream()) {
                byte[] resourceBytes = stream.readAllBytes();
                Files.write(outPath, resourceBytes);
                getLog().info( String.format("resource file written: ", outPath));
            } catch(Exception e) {
                getLog().error(String.format("resource file failed: ", outPath), e);
            }
        }
    }

    private File createDacDocResourceDir(File baseDir) {
        File destDacDocResourceDirectory = Path.of(baseDir.getAbsolutePath(), Constants.DACDOC_RESOURCES).toFile();
        getLog().info( String.format("DacDoc resource directory: %s", destDacDocResourceDirectory.getAbsolutePath()));

        if(!destDacDocResourceDirectory.exists()) {
            destDacDocResourceDirectory.mkdir();
            getLog().info( String.format("DacDoc resource directory created: %s", destDacDocResourceDirectory.getAbsolutePath()));
        }
        return destDacDocResourceDirectory;
    }
}
