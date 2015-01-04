package bigtree

import org.gradle.api.Project

class NodeEnvExtension {
  
  String ver = '0.10.35'  
    
  String extractPath = 'node'
    
  String downloadBaseUrl = 'http://nodejs.org/dist/'
  
  String defaultModules //= 'rubygems-update rake bundler'
  
  String npmDownloadBaseUrl = 'https://codeload.github.com/npm/npm/zip'
  
  String npmVer = '2.1.17'
  
  //
  Project project
  
  def getExtractPath() {
    if (null == extractPath || 0 == extractPath.length()) {
      return project.projectDir.getAbsolutePath()        
    }
    new File(project.projectDir, extractPath).getAbsolutePath()
  }  

  def getNodeHome() {
    getExtractPath() + '/' + ver
  }  
}
