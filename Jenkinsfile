stage 'Load pipeline from GitHub'
def pipeline = fileLoader.fromGit('android-pipeline', 
        'https://github.com/SpartaHacks/Pipeline.git', 'master', null, '')

stage 'Run method from the Pipeline file'
pipeline.jenkinsBuild()
