GIVEN METHOD_CALL $getUploadFilename WHERE
  name = getName
  arguments.size = 0
  type = org.apache.commons.fileupload.FileItem OR type = org.apache.commons.fileupload.FileItem
  enclosingStatement.code !=~ validate
  enclosingStatement.code !=~ toSimpleFileName

TRANSFORM
  RETURN org.pixee.security.SafeIO.toSimpleFileName($getUploadFilename)
