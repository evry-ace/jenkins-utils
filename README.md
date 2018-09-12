# jenkins-utils
Jenkins Pipeline Utility Library for Kubernetes Deployments

# s3CmdUpload
```
  def opts = [
      host: "127.0.0.1",
      secret: "Supersecret
  ]

  # Run raw commands towards 
  s3Cmd("mb s3://myfiles", opts)
  s3Cmd("upload foo.txt s3://myfiles", opts)

  # Or use utility helpers
  s3UploadCmd("foo.txt", "mybucket", "/subpath.txt", opts)
  s3UploadCmd("myfolder", "mybucket", "/remotefolder", recurse=true, opts=opts)
}
```

