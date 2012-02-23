var foo = {hi:"hola"};
var bar = {hi:"hello"};
(function(scope) {
  if(scope["CFInstall"]) {
    return
  }
  var byId = function(id, doc) {
    return typeof id == "string" ? (doc || document).getElementById(id) : id
  };
  var isAvailable = function() {
    if(scope.CFInstall._force) {
      return scope.CFInstall._forceValue
    }
    var ua = navigator.userAgent.toLowerCase();
    if(ua.indexOf("chromeframe") >= 0) {
      return true
    }
    if(typeof window["ActiveXObject"] != "undefined") {
      try {
        var obj = new ActiveXObject("ChromeTab.ChromeFrame");
        if(obj) {
          return true
        }
      }catch(e) {
      }
    }
    return false
  };
  var injectStyleSheet = function(rules) {
    try {
      var ss = document.createElement("style");
      ss.setAttribute("type", "text/css");
      if(ss.styleSheet) {
        ss.styleSheet.cssText = rules
      }else {
        ss.appendChild(document.createTextNode(rules))
      }
      var h = document.getElementsByTagName("head")[0];
      var firstChild = h.firstChild;
      h.insertBefore(ss, firstChild)
    }catch(e) {
    }
  };
  var cfStyleTagInjected = false;
  var cfHiddenInjected = false;
  var injectCFStyleTag = function() {
    if(cfStyleTagInjected) {
      return
    }
    var rules = ".chromeFrameInstallDefaultStyle {" + "width: 800px;" + "height: 600px;" + "position: absolute;" + "left: 50%;" + "top: 50%;" + "margin-left: -400px;" + "margin-top: -300px;" + "}" + ".chromeFrameOverlayContent {" + "position: absolute;" + "margin-left: -400px;" + "margin-top: -300px;" + "left: 50%;" + "top: 50%;" + "border: 1px solid #93B4D9;" + "background-color: white;" + "z-index: 2001;" + "}" + ".chromeFrameOverlayContent iframe {" + "width: 800px;" + "height: 600px;" + "border: none;" +
    "}" + ".chromeFrameOverlayCloseBar {" + "height: 1em;" + "text-align: right;" + "background-color: #CADEF4;" + "}" + ".chromeFrameOverlayUnderlay {" + "position: absolute;" + "width: 100%;" + "height: 100%;" + "background-color: white;" + "opacity: 0.5;" + "-moz-opacity: 0.5;" + "-webkit-opacity: 0.5;" + "-ms-filter: " + '"progid:DXImageTransform.Microsoft.Alpha(Opacity=50)";' + "filter: alpha(opacity=50);" + "z-index: 2000;" + "}";
    injectStyleSheet(rules);
    cfStyleTagInjected = true
  };
  var closeOverlay = function() {
    if(cfHiddenInjected) {
      return
    }
    var rules = ".chromeFrameOverlayContent { display: none; }" + ".chromeFrameOverlayUnderlay { display: none; }";
    injectStyleSheet(rules);
    var age = 365 * 24 * 60 * 60 * 1E3;
    document.cookie = "disableGCFCheck=1;path=/;max-age=" + age;
    cfHiddenInjected = true
  };
  var setProperties = function(node, args) {
    var srcNode = byId(args["node"]);
    node.id = args["id"] || (srcNode ? srcNode["id"] || getUid(srcNode) : "");
    var cssText = args["cssText"] || "";
    node.style.cssText = " " + cssText;
    var classText = args["className"] || "";
    node.className = classText;
    var src = args["src"] || "about:blank";
    node.src = src;
    if(srcNode) {
      srcNode.parentNode.replaceChild(node, srcNode)
    }
  };
  var makeIframe = function(args) {
    var el = document.createElement("iframe");
    el.setAttribute("frameborder", "0");
    el.setAttribute("border", "0");
    setProperties(el, args);
    return el
  };
  var makeInlinePrompt = function(args) {
    args.className = "chromeFrameInstallDefaultStyle " + (args.className || "");
    var ifr = makeIframe(args);
    if(!ifr.parentNode) {
      var firstChild = document.body.firstChild;
      document.body.insertBefore(ifr, firstChild)
    }
  };
  var makeOverlayPrompt = function(args) {
    if(byId("chromeFrameOverlayContent")) {
      return
    }
    var n = document.createElement("span");
    n.innerHTML = '<div class="chromeFrameOverlayUnderlay"></div>' + '<table class="chromeFrameOverlayContent"' + 'id="chromeFrameOverlayContent"' + 'cellpadding="0" cellspacing="0">' + '<tr class="chromeFrameOverlayCloseBar">' + "<td>" + '<button id="chromeFrameCloseButton">close</button>' + "</td>" + "</tr>" + "<tr>" + '<td id="chromeFrameIframeHolder"></td>' + "</tr>" + "</table>";
    for(var b = document.body;n.firstChild;) {
      b.insertBefore(n.lastChild, b.firstChild)
    }
    var ifr = makeIframe(args);
    byId("chromeFrameIframeHolder").appendChild(ifr);
    byId("chromeFrameCloseButton").onclick = closeOverlay
  };
  var CFInstall = {};
  CFInstall.check = function(args) {
    args = args || {};
    var ua = navigator.userAgent;
    var ieRe = /MSIE (\S+); Windows NT/;
    var bail = false;
    if(ieRe.test(ua)) {
      if(parseFloat(ieRe.exec(ua)[1]) < 6 && ua.indexOf("SV1") < 0) {
        bail = true
      }
    }else {
      bail = true
    }
    if(bail) {
      return
    }
    injectCFStyleTag();
    if(document.cookie.indexOf("disableGCFCheck=1") >= 0) {
      closeOverlay()
    }
    var currentProtocol = document.location.protocol;
    var protocol = currentProtocol == "https:" ? "https:" : "http:";
    var installUrl = protocol + "//www.google.com/chromeframe";
    if(!isAvailable()) {
      if(args.onmissing) {
        args.onmissing()
      }
      args.src = args.url || installUrl;
      var mode = args.mode || "inline";
      var preventPrompt = args.preventPrompt || false;
      if(!preventPrompt) {
        if(mode == "inline") {
          makeInlinePrompt(args)
        }else {
          if(mode == "overlay") {
            makeOverlayPrompt(args)
          }else {
            window.open(args.src)
          }
        }
      }
      if(args.preventInstallDetection) {
        return
      }
      var installTimer = setInterval(function() {
        if(isAvailable()) {
          if(args.oninstall) {
            args.oninstall()
          }
          clearInterval(installTimer);
          window.location = args.destination || window.location
        }
      }, 2E3)
    }
  };
  CFInstall._force = false;
  CFInstall._forceValue = false;
  CFInstall.isAvailable = isAvailable;
  scope.CFInstall = CFInstall
})(this["ChromeFrameInstallScope"] || this);
