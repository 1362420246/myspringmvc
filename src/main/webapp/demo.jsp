<%@ page language="java" import="java.util.*" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="zh-hans">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="ie=edge">
  <title>Document</title>
  <link href="https://cdn.bootcss.com/bootstrap/4.0.0/css/bootstrap.css" rel="stylesheet">
  <script src="https://cdn.bootcss.com/vue/2.5.15/vue.js"></script>
  <style>
    body {overflow:hidden;}
    textarea {
      width: 400px;
      height: 100px;
      overflow: auto;
      vertical-align: top;
    }
    .box {
      display: flex;
    }
    .control, .view {
      width: 50%;
    }
    .view li {
      height: 95vh;
      overflow: auto;
    }
    .list-group {
      height: 95vh;
      overflow: auto;
    }
    h2 {height: 5vh;line-height: 5vh;}
  </style>
</head>
<body>
  <div id="app">
    <h2>一只DEMO</h2>
    <div class="box">
      <ul class="list-group control">
        <li class="list-group-item">
          <h3>请求地址</h3>
          <p>例如：ie/info.do</p>
          <label style="width:100%">
            <input style="width:100%" id="ip">
          </label>
        </li>
        <li class="list-group-item">
          <h3>发送数据</h3>
          <!-- <p>温馨提示：传入数据如果为JSON格式的话，需要调用JSON编译方法JSON.stringify()，括号内为需要编译的代码</p> -->
          <p v-show="onoff">例如：
            {
              "name": "liming",
              "data":"{\"weight\":70,\"height\":175}"},
              "sex": "men"
            } 
          </p>
          <p v-show="!onoff">
            例如： data: JSON.stringify({weight:70, height: 175})
          </p>
          <p>
            <a href="javascript:;" v-show="onoff" @click="onoff = !onoff">切换精简模式</a>
            <a href="javascript:;" v-show="!onoff" @click="onoff = !onoff">切换经典模式</a>
          </p>
          <p v-show="onoff">
            <label>
              <textarea v-model="JSON.stringify(req)" @change="change()" ref="textarea"></textarea>
            </label>
          </p>
          <table v-show="!onoff" class="table table-hover">
            <thead>
              <tr>
                <th>参数名</th>
                <th>值</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(val, key) in req">
                <td>{{key}}</td>
                <td>{{val}}</td>
              </tr>
            </tbody>
          </table>
          <p>
            <label>
              <input type="text" v-model="key">
              <input type="text" v-model="val">
            </label>
            <button @click="add" class="btn btn-primary btn-xs">添加参数</button>
          </p>
        </li>
        <li class="list-group-item">
          <span>请求方式</span>
          <select class="custom-select" style="width: 100px" id="type">
            <option value="POST">POST</option>
            <option value="GET">GET</option>
          </select>
        </li>
        <li class="list-group-item">
          <button id="go" class="btn btn-primary btn-lg">发送请求</button>
        </li>
      </ul>
      <ul class="list-group view">
        <li class="list-group-item">
          <h3>请求到的数据：</h3>
          <pre id="content"></pre>
        </li>
      </ul>
    </div>
  </div>
  <script src="https://cdn.bootcss.com/jquery/1.11.2/jquery.min.js"></script>
  <script src="https://cdn.bootcss.com/layer/3.1.0/layer.js"></script>
  <script>
    // 格式化json代码
    var formatJson = function(json, options) {
      var reg = null,
        formatted = '',
        pad = 0,
        PADDING = '    '; // one can also use '\t' or a different number of spaces
    
      // optional settings
      options = options || {};
      // remove newline where '{' or '[' follows ':'
      options.newlineAfterColonIfBeforeBraceOrBracket = (options.newlineAfterColonIfBeforeBraceOrBracket === true) ? true : false;
      // use a space after a colon
      options.spaceAfterColon = (options.spaceAfterColon === false) ? false : true;
    
      // begin formatting...
      if (typeof json !== 'string') {
        // make sure we start with the JSON as a string
        json = JSON.stringify(json);
      } else {
        // is already a string, so parse and re-stringify in order to remove extra whitespace
        json = JSON.parse(json);
        json = JSON.stringify(json);
      }
    
      // add newline before and after curly braces
      reg = /([\{\}])/g;
      json = json.replace(reg, '\r\n$1\r\n');
    
      // add newline before and after square brackets
      reg = /([\[\]])/g;
      json = json.replace(reg, '\r\n$1\r\n');
    
      // add newline after comma
      reg = /(\,)/g;
      json = json.replace(reg, '$1\r\n');
    
      // remove multiple newlines
      reg = /(\r\n\r\n)/g;
      json = json.replace(reg, '\r\n');
    
      // remove newlines before commas
      reg = /\r\n\,/g;
      json = json.replace(reg, ',');
    
      // optional formatting...
      if (!options.newlineAfterColonIfBeforeBraceOrBracket) {			
        reg = /\:\r\n\{/g;
        json = json.replace(reg, ':{');
        reg = /\:\r\n\[/g;
        json = json.replace(reg, ':[');
      }
      if (options.spaceAfterColon) {			
        reg = /\:/g;
        json = json.replace(reg, ':');
      }
    
      $.each(json.split('\r\n'), function(index, node) {
        var i = 0,
          indent = 0,
          padding = '';
    
        if (node.match(/\{$/) || node.match(/\[$/)) {
          indent = 1;
        } else if (node.match(/\}/) || node.match(/\]/)) {
          if (pad !== 0) {
            pad -= 1;
          }
        } else {
          indent = 0;
        }
    
        for (i = 0; i < pad; i++) {
          padding += PADDING;
        }
    
        formatted += padding + node + '\r\n';
        pad += indent;
      });
    
      return formatted;
    };
  </script>
  <script>
    var vm = new Vue({
      el: '#app',
      data () {
        return {
          onoff: true,
          req: {},
          key: '',
          val: '',
          control_h: $('.control').css('height')
        }
      },
      methods: {
        add: function() {
          Vue.set(this.req, this.key, eval(this.val));
          this.key = '';
          this.val = '';
        },
        change: function() {
          this.req = eval(JSON.parse(this.$refs.textarea.value));
        }
      }
    })
  </script>
  <script>
    var resultData;
    $('#go').click(function() {
      var data = eval("({"+$('#data').val()+"})");
      if (!data) return console.log(data);
      if (typeof data !== 'object') return console.log('data: ', typeof data);
      var ip = $('#ip').val();
      var load = layer.load();
      $.ajax({
        url: ip,
        data: vm.req,
        dataType: 'json',
        type: $('#type').val(),
        success: function(data) {
          $('#content').html(formatJson(data));
        },
        error: function(data) {
          $('#content').html(formatJson(data));
        },
        complete: function() {
          layer.close(load);
        }
      });
    });
  </script>
</body>
</html>