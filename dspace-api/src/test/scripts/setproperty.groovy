/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
project.properties.setProperty('agnostic.build.dir', project.build.directory.replace('\\','/'));
println("Initializing Maven property 'agnostic.build.dir' to: " + project.properties['agnostic.build.dir']);
