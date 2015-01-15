package bigtree

import org.gradle.api.Project

class NodeProjectExtension {
  
  String nameWithPath
    
  // 是否为 nw.js(node-webkit) 项目
  boolean isNwJsProject = false  
  
  // nw.js 版本
  String nwJsVer = '0.11.5'
  
  String defaultModules = ''
  
  //
  Project project 
  
  def getNameWithPath() {
    if (null == nameWithPath || 0 >= nameWithPath.length()) {
      return 'src/main/javaScript/nodeProject'
    }
    nameWithPath
  }  
  
}
