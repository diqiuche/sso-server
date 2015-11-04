<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>config</title>
<script type="text/javascript" src="${appctx}/scripts/jquery.js"></script>
<script type="text/javascript">

	function refreshConfig() {
		$.ajax({
			url:"${appctx}/config/refresh",
			data: {code: $("#code").val()}
		}).done(function(success) {
			alert(success ? "更新成功" : "code error");
		}).fail(function() {
			alert("系统错误")
		});
	}
</script>
</head>
<body>
<input type="text" id="code" >
<button onclick="refreshConfig();">更新配置</button> 
</body>
</html>