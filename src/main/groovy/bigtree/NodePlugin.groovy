package bigtree

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.BasePlugin

class NodePlugin implements Plugin<Project> {
  
  void apply(Project project) {
    project.plugins.apply(BasePlugin)
    def nodeEnv = project.extensions.create('nodeEnv', NodeEnvExtension)
    nodeEnv.project = project
    
    def nodeProject = project.extensions.create('nodeProject', NodeProjectExtension)
    nodeProject.project = project    
    
    project.task('build') {
      description  'Assembles and tests this project.'    
      group  'build'
      dependsOn project.assemble
    }      
    
    project.task('installNode') << {
      description 'Install Node' 
      installNode(project)      
    }
    
    project.task('reinstallNode') << {
      description 'Reinstall Node, will delete installed Node previously'
      uninstallNode(project)
      installNode(project)
    }    
    
    project.task('uninstallNode') << {
      description 'Uninstall Node, will delete installed Node'
      uninstallNode(project)
    }
    
    project.task('exec') << {  
      if (project.hasProperty('cmds')) {
        def cmds = project.cmds
        cmds.split(';').each {
          def cmd = "${it}"
          exec(project, project.nodeProject.nameWithPath, cmd)
        }          
      }
    }
    
    project.task('npm') << {
      def args = project.hasProperty('args') ? project.args : ''
      if (null == args || 0 >= args.length()) {
        args = 'list'
      }
      def cmd = "${args}"
      def executable = getNpmExecutableWithPath(project)
      project.ant.exec(dir: project.nodeProject.nameWithPath, 
                       executable: executable) {
                         arg(line: cmd)      
      }
    } 

    project.task('newProject') << {  
      final nodeProjectDir = project.file(project.nodeProject.nameWithPath)
      if (!nodeProjectDir.exists()) {
        nodeProjectDir.mkdirs()
      }
      
      // def cmd = 'init'
      // def executable = getNpmExecutableWithPath(project)
      // project.ant.exec(dir: nodeProjectDir, executable: executable, spawn: 'true') {
      //   arg(line: cmd)      
      // }
      
      final packageFile = new File(nodeProjectDir, "package.json")
      if (!packageFile.exists()) {
        packageFile << generatePackageJson()
      }
      
      def modules = project.nodeProject.defaultModules
      if (null == modules) {
        modules = ""
      }      
      if (project.nodeProject.isNwJsProject) {
        exec(project, project.nodeProject.nameWithPath, ('npm install node-webkit-builder -g'))
      }
      
      if (0 < modules.length()) {
        exec(project, project.nodeProject.nameWithPath, ('npm install ' + modules + ' --save'))
      }
    }    
  }
  
  def installNodeOnOsX(project) {
    if(is32Arch(project.ant)) {
      throw new UnsupportedOperationException('DO NOT support darwin-x86')
    }
    
    final ant = project.ant 
    final nodeEnv = project.nodeEnv
    final tmpPath = nodeEnv.installPath + '/tmp'
    ant.mkdir(dir: tmpPath)
    
    final fileName = "node-v${nodeEnv.ver}-darwin-x64.tar.gz"
    def nodeUrl = nodeEnv.downloadBaseUrl + '/v' + nodeEnv.ver + '/' + fileName

    // 下载到临时目录，如果文件不存在
    def nodeName = nodeEnv.ver + '-' + nodeUrl.split('/')[-1]
    final nodeDest = "${tmpPath}/${nodeName}"
    if (!project.file(nodeDest).exists()) {
      ant.get(src: nodeUrl, dest: nodeDest, verbose: true)
    }
    
    // 放置到 Node Home
    final nodeHome = nodeEnv.getNodeHome()
    nodeName = nodeName.split('-')[-1]
    ant.gunzip(src: nodeDest)   
    ant.untar(src: nodeDest.replaceAll('.gz', ''), dest: nodeEnv.installPath)
    ant.move(file: nodeEnv.installPath + '/' + fileName.replaceAll('.tar.gz', ''), tofile: nodeHome)
    
    // 安装npm
    def cmd = "npm"
    project.ant.exec(dir: "${project.nodeEnv.getNodeHome()}/bin", 
    executable: 'rm', 
    osfamily: 'unix') {
      arg(line: cmd)      
    }
    cmd = "-s ../lib/node_modules/npm/bin/npm-cli.js npm "
    project.ant.exec(dir: "${project.nodeEnv.getNodeHome()}/bin", 
    executable: 'ln', 
    osfamily: 'unix') {
      arg(line: cmd)      
    }    
    
    // 设置可执行权限
    setExecutable(project)      
  }
  
  def installNodeOnWindows(project) {
    final ant = project.ant 
    final nodeEnv = project.nodeEnv
    final tmpPath = nodeEnv.installPath + '/tmp'
    ant.mkdir(dir: tmpPath)
    
    def nodeUrl = nodeEnv.downloadBaseUrl + '/v' + nodeEnv.ver
    // 32位
    if(is32Arch(project.ant)) {
      nodeUrl = "${nodeUrl}/node.exe"
    }// 64位 
    else {     
      nodeUrl = "${nodeUrl}/x64/node.exe"        
    }

    // 下载到临时目录，如果文件不存在
    def nodeName = nodeEnv.ver + '-' + nodeUrl.split('/')[-1]
    final nodeDest = "${tmpPath}/${nodeName}"
    if (!project.file(nodeDest).exists()) {
      ant.get(src: nodeUrl, dest: nodeDest, verbose: true)
    }
    final npmDest = "${tmpPath}/npm-${nodeEnv.npmVer}.zip"
    if (!project.file(npmDest).exists()) {
      def npmUrl = nodeEnv.npmDownloadBaseUrl + '/v' + nodeEnv.npmVer
      ant.get(src: npmUrl, dest: npmDest, verbose: true)
    }
    
    // 放置到 Node Home
    final nodeHome = nodeEnv.getNodeHome()
    ant.mkdir(dir: nodeHome)
    nodeName = nodeName.split('-')[-1]
    ant.copy(file: nodeDest, tofile: "${nodeHome}/${nodeName}")   
    ant.unzip(src: npmDest, dest: tmpPath)

    // 安装npm
    def executable = getNodeExecutableWithPath(project)
    project.ant.exec(dir: "${tmpPath}/npm-${nodeEnv.npmVer}", executable: executable) {
      arg(line: 'cli.js install -g')
    }     
  }   
  
  // *nix下，要设置这些脚本的可执行权限
  def setExecutable(project) {
    def cmd = "500 node npm"
    project.ant.exec(dir: "${project.nodeEnv.getNodeHome()}/bin", 
    executable: 'chmod', 
    osfamily: 'unix') {
      arg(line: cmd)      
    }     
  }    
  
  def getExecutableWithPath(project, cmd) {
    def executable = isWindows() ? "${cmd}.cmd" : "bin/${cmd}"
    "${project.nodeEnv.getNodeHome()}/${executable}"
  }
  
  def getNodeExecutableWithPath(project) {
    def executable = isWindows() ? "node.exe" : "bin/node"
    "${project.nodeEnv.getNodeHome()}/${executable}"
  }
  
  def getNpmExecutableWithPath(project) {
    def executable = isWindows() ? "npm.cmd" : "bin/npm"
    "${project.nodeEnv.getNodeHome()}/${executable}"
  }  
  
  def installDefaultGlobalModules(project) {
    final modules = project.nodeEnv.defaultGlobalModules
    if (null != modules && 0 < modules.length()) {
      def cmd = 'install ' + modules + ' -g'
      def executable = getNpmExecutableWithPath(project)
      project.ant.exec(dir: project.nodeEnv.getNodeHome(), executable: executable) {
        arg(line: cmd)      
      } 
    }
  }  
  
  def exec(project, path, cmd) {
    final cmdWithArgs = cmd.split()
    def args = ''
    if (1 < cmdWithArgs.length) {
      args = cmdWithArgs[1..-1].join(' ')
    }
    def executable = getExecutableWithPath(project, cmdWithArgs[0])
    project.ant.exec(dir: path, 
                     executable: executable) {
                       arg(line: args)
    }    
  }
  
  def installNode(project) {
    if(isWindows()) {
      installNodeOnWindows(project) 
    } else if(isOsX()) {
      installNodeOnOsX(project)
    } else if(isLinux()) { 
      throw new UnsupportedOperationException('DO NOT support this platform')        
    } else {
      throw new UnsupportedOperationException('DO NOT support this platform')        
    }
    installDefaultGlobalModules(project)
  }
  
  def uninstallNode(project) {  
    if(!project.hasProperty('cleanAllNode')) {
      project.ant.delete(dir: project.nodeEnv.getNodeHome())
    } else {
      project.ant.delete(dir: project.nodeEnv.installPath)
    }      
  }
  
  static isWindows() {
    Os.isFamily(Os.FAMILY_WINDOWS)
  }
  
  static isOsX() {
    Os.isFamily(Os.FAMILY_MAC)
  }
  
  static isLinux() {
    Os.isFamily(Os.FAMILY_UNIX)
  }
  
  static is32Arch(ant) {
    'x86' == ant.properties['os.arch']
  }
  
  // 生成 package.json 文件内容
  static generatePackageJson() {
"""{
  "name": "TODOProjectName",
  "private": true,
  "version": "0.0.1",
  "description": "TODO project description",
  "keywords": [],
  "dependencies": {
  },
  "devDependencies": {
  },
  "scripts": {
    "start": "node app.js"
  },
  "main": "app.js"
}
"""
  }
}
