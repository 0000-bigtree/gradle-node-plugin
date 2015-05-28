Node Plugin for Gradle
================================================================================

    buildscript {  
      repositories {
        mavenLocal()
      }
        
      dependencies {
        classpath "bigtree:nodePlugin:0.1.0"
      }
    }
    
    apply plugin: 'bigtree.node'
    
    nodeEnv {
      ver = '0.12.4'
      
      npmVer = '2.10.1'
      defaultGlobalModules = 'gulp'
    }
    
    nodeProject {
      nameWithPath = '.'
      defaultModules = 'gulp log4js'
    }
