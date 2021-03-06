package bigtree

import org.gradle.api.Project

class NodeEnvExtension {
  
  String ver = '0.12.7'
  
  String installPath = 'node'
  
  String downloadBaseUrl = 'http://nodejs.org/dist'
  
  String defaultGlobalModules = ''
  
  String npmDownloadBaseUrl = 'https://codeload.github.com/npm/npm/zip'
  
  String npmVer = '2.13.4'
  
  //
  Project project
  
  def getInstallPath() {
    if (null == installPath || 0 == installPath.length()) {
      return project.projectDir.getAbsolutePath()        
    }
    new File(project.projectDir, installPath).getAbsolutePath()
  }  
  
  def getNodeHome() {
    getInstallPath() + '/' + ver
  }  
}

