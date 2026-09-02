package com.jhaiian.clint.userscripts

import android.content.Context
import org.json.JSONObject

object UserScriptEngine {

    private const val RUNTIME_BOOTSTRAP = "if(!window.__usRuntime){" +
        "window.__usRuntime={xhrCallbacks:{},valueListeners:{},menuCommands:{}};" +
        "window.__usXhrCallback=function(id,result,error){var c=window.__usRuntime.xhrCallbacks[id];if(c){delete window.__usRuntime.xhrCallbacks[id];c(result,error);}};" +
        "window.__usValueChanged=function(scriptKey,key,jsonValueOrNull){var list=window.__usRuntime.valueListeners[scriptKey];if(!list)return;" +
        "var nv=(jsonValueOrNull===null||jsonValueOrNull===undefined)?undefined:JSON.parse(jsonValueOrNull);" +
        "for(var k in list){if(list.hasOwnProperty(k)){try{list[k](key,undefined,nv,false);}catch(e){}}}};" +
        "}"

    fun buildCombinedScript(context: Context): String? {
        if (!UserScriptState.isEnabled(context)) return null
        val scripts = UserScriptDatabase(context).getAll().filter { it.enabled }
        if (scripts.isEmpty()) return null
        val builder = StringBuilder()
        builder.append(RUNTIME_BOOTSTRAP).append('\n')
        for (script in scripts) {
            builder.append(buildScriptBlock(script))
            builder.append('\n')
        }
        return builder.toString()
    }

    private fun quote(value: String): String = JSONObject.quote(value)

    private fun buildScriptBlock(script: UserScript): String {
        val meta = UserScriptMetadataParser.parse(script.code, "Untitled Script")
        val excludePatterns = meta.excludes.map { UserScriptMatcher.globToRegexSource(it) } +
            meta.excludeMatches.mapNotNull { UserScriptMatcher.matchPatternToRegexSource(it) }
        val matchPatterns = meta.matches.mapNotNull { UserScriptMatcher.matchPatternToRegexSource(it) } +
            meta.includes.map { UserScriptMatcher.globToRegexSource(it) }
        val requireSources = UserScriptRequireFetcher.parseSources(script.requiresCache)
        val resources = UserScriptRequireFetcher.parseResources(script.requiresCache)

        val excludeArray = excludePatterns.joinToString(",") { "new RegExp(${quote(it)})" }
        val matchArray = matchPatterns.joinToString(",") { "new RegExp(${quote(it)})" }

        val requireBlock = requireSources.joinToString("\n") {
            "try{\n$it\n}catch(__reqErr){console.error('[UserScript] require failed',__reqErr);}"
        }

        val resourcesObj = JSONObject()
        for (res in resources) {
            resourcesObj.put(res.name, JSONObject().put("mime", res.mime).put("base64", res.base64).put("url", res.url))
        }

        val connectsArray = meta.connects.joinToString(",") { quote(it) }
        val grantsArray = meta.grants.joinToString(",") { quote(it) }

        return "(function(){" +
            "var __url=location.href;" +
            "var __exclude=[$excludeArray];" +
            "var __match=[$matchArray];" +
            "for(var __i=0;__i<__exclude.length;__i++){if(__exclude[__i].test(__url))return;}" +
            "if(__match.length>0){var __ok=false;for(var __j=0;__j<__match.length;__j++){if(__match[__j].test(__url)){__ok=true;break;}}if(!__ok)return;}" +
            "if(${meta.noframes}&&window.self!==window.top)return;" +
            "var __scriptId=${script.id};" +
            "var __scriptKey=${quote("us_${script.id}")};" +
            "var __scriptName=${quote(meta.name)};" +
            "var __resources=$resourcesObj;" +
            "var GM_info={" +
            "script:{name:${quote(meta.name)},namespace:${quote(meta.namespace)},version:${quote(meta.version)}," +
            "description:${quote(meta.description)},author:${quote(meta.author)}," +
            "matches:__match.map(function(r){return r.source;})," +
            "includes:[],excludes:[],resources:__resources,connects:[$connectsArray],grant:[$grantsArray]," +
            "runAt:${quote(meta.runAt)},noframes:${meta.noframes},unwrap:${meta.unwrap}}," +
            "version:'1.0',scriptHandler:'ClintBrowser',scriptMetaStr:null,scriptWillUpdate:false,isIncognito:false,downloadMode:'native'};" +
            "var unsafeWindow=window;" +
            "function GM_log(){try{console.log.apply(console,arguments);}catch(__e){}}" +
            "function GM_addStyle(css){function __apply(){var __s=document.createElement('style');__s.setAttribute('data-userscript',__scriptName);__s.textContent=css;(document.head||document.documentElement).appendChild(__s);return __s;}if(document.head||document.documentElement)return __apply();document.addEventListener('DOMContentLoaded',__apply);}" +
            "function GM_addElement(a,b,c){var __tag,__attrs,__parent;if(typeof a==='string'){__parent=document.head||document.documentElement;__tag=a;__attrs=b||{};}else{__parent=a;__tag=b;__attrs=c||{};}var __el=document.createElement(__tag);for(var __k in __attrs){if(__attrs.hasOwnProperty(__k)){if(__k==='textContent')__el.textContent=__attrs[__k];else __el.setAttribute(__k,__attrs[__k]);}}__parent.appendChild(__el);return __el;}" +
            "function GM_getValue(key,def){try{var __v=ClintUserScriptBridge.getValue(__scriptKey,key);return (__v===null||__v===undefined)?def:JSON.parse(__v);}catch(__e){return def;}}" +
            "function GM_setValue(key,value){try{ClintUserScriptBridge.setValue(__scriptKey,key,JSON.stringify(value===undefined?null:value));}catch(__e){}}" +
            "function GM_deleteValue(key){try{ClintUserScriptBridge.deleteValue(__scriptKey,key);}catch(__e){}}" +
            "function GM_listValues(){try{return JSON.parse(ClintUserScriptBridge.listValues(__scriptKey));}catch(__e){return [];}}" +
            "var __gmListenerSeq=0;" +
            "function GM_addValueChangeListener(name,cb){var __id='l'+(++__gmListenerSeq);window.__usRuntime.valueListeners[__scriptKey]=window.__usRuntime.valueListeners[__scriptKey]||{};window.__usRuntime.valueListeners[__scriptKey][__id]=function(k,ov,nv,rm){if(k===name)cb(name,ov,nv,rm);};return __id;}" +
            "function GM_removeValueChangeListener(id){var __l=window.__usRuntime.valueListeners[__scriptKey];if(__l)delete __l[id];}" +
            "function GM_openInTab(url,opts){try{var __feat=(opts&&opts.active===false)?'noopener':'noopener';var __w=window.open(url,'_blank',__feat);return {closed:false,close:function(){if(__w)__w.close();},onclose:null};}catch(__e){return null;}}" +
            "function GM_setClipboard(text,type){try{if(navigator.clipboard&&navigator.clipboard.writeText){navigator.clipboard.writeText(text);return;}}catch(__e){}try{var __ta=document.createElement('textarea');__ta.value=text;__ta.style.position='fixed';__ta.style.opacity='0';document.body.appendChild(__ta);__ta.focus();__ta.select();document.execCommand('copy');document.body.removeChild(__ta);}catch(__e2){}}" +
            "function GM_notification(details,ondone){try{var __opts=(typeof details==='string')?{text:details,title:ondone}:(details||{});" +
            "if(typeof __opts.title!=='string')__opts.title=__scriptName;" +
            "ClintUserScriptBridge.notify(__scriptName,__opts.title,__opts.text||'',location.hostname||'');" +
            "if(typeof __opts.ondone==='function')setTimeout(__opts.ondone,0);}catch(__e){}}" +
            "var __xhrSeq=0;" +
            "function GM_xmlhttpRequest(details){details=details||{};try{var __id='x'+(++__xhrSeq)+'_'+Date.now();" +
            "var __payload={url:details.url,method:(details.method||'GET').toUpperCase(),headers:details.headers||{}};" +
            "if(details.data!==undefined&&details.data!==null)__payload.data=(typeof details.data==='string')?details.data:JSON.stringify(details.data);" +
            "window.__usRuntime.xhrCallbacks[__id]=function(result,error){" +
            "if(error){if(details.onerror)details.onerror({error:error.error||'error',finalUrl:details.url});return;}" +
            "var __resp=result.responseText;var __respObj=__resp;" +
            "if(details.responseType==='json'){try{__respObj=JSON.parse(__resp);}catch(__pe){__respObj=null;}}" +
            "var __r={status:result.status,statusText:result.statusText,responseText:__resp,response:__respObj,readyState:4,finalUrl:result.finalUrl,responseHeaders:result.responseHeaders};" +
            "if(details.onreadystatechange)try{details.onreadystatechange(__r);}catch(__e3){}" +
            "if(result.status>=200&&result.status<400){if(details.onload)details.onload(__r);}else{if(details.onerror)details.onerror(__r);}" +
            "};" +
            "ClintUserScriptBridge.xhr(__id,JSON.stringify(__payload));" +
            "return {abort:function(){try{ClintUserScriptBridge.abort(__id);}catch(__ae){}if(details.onabort)details.onabort();}};" +
            "}catch(__e){if(details.onerror)details.onerror({error:String(__e)});return {abort:function(){}};}}" +
            "function GM_download(details,filename){try{var __opts=(typeof details==='string')?{url:details,name:filename}:(details||{});" +
            "fetch(__opts.url,{credentials:'same-origin'}).then(function(__r){return __r.blob();}).then(function(__b){" +
            "var __reader=new FileReader();__reader.onloadend=function(){var __b64=String(__reader.result).split(',')[1]||'';" +
            "ClintUserScriptBridge.download(__b64,__opts.name||'download',__b.type||'application/octet-stream');if(__opts.onload)__opts.onload();};" +
            "__reader.readAsDataURL(__b);}).catch(function(__e){if(__opts.onerror)__opts.onerror(String(__e));});}catch(__e){if(typeof filename==='object'&&filename.onerror)filename.onerror(String(__e));}}" +
            "function GM_getResourceText(name){var __r=__resources[name];if(!__r)return undefined;try{return decodeURIComponent(escape(atob(__r.base64)));}catch(__e){try{return atob(__r.base64);}catch(__e2){return undefined;}}}" +
            "function GM_getResourceURL(name){var __r=__resources[name];if(!__r)return undefined;return 'data:'+__r.mime+';base64,'+__r.base64;}" +
            "function GM_registerMenuCommand(caption,onClick,accessKey){var __id='m'+(++__xhrSeq);window.__usRuntime.menuCommands[__id]={scriptName:__scriptName,caption:caption,onClick:onClick,accessKey:accessKey};__usRenderMenuButton();return __id;}" +
            "function GM_unregisterMenuCommand(id){delete window.__usRuntime.menuCommands[id];__usRenderMenuButton();}" +
            "function __usRenderMenuButton(){try{var __ids=Object.keys(window.__usRuntime.menuCommands);var __btn=document.getElementById('__us_menu_btn');" +
            "if(__ids.length===0){if(__btn)__btn.remove();return;}" +
            "if(!__btn){__btn=document.createElement('div');__btn.id='__us_menu_btn';" +
            "__btn.style.cssText='position:fixed;right:12px;bottom:76px;z-index:2147483647;width:36px;height:36px;border-radius:18px;background:rgba(32,32,36,0.85);color:#fff;display:flex;align-items:center;justify-content:center;font:14px sans-serif;box-shadow:0 2px 8px rgba(0,0,0,0.4);cursor:pointer;user-select:none;';" +
            "__btn.textContent='\u2699';" +
            "var __menu=document.createElement('div');__menu.id='__us_menu_list';" +
            "__menu.style.cssText='position:fixed;right:12px;bottom:118px;z-index:2147483647;background:#1f1f23;color:#fff;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,0.5);display:none;min-width:160px;font:13px sans-serif;overflow:hidden;';" +
            "document.documentElement.appendChild(__btn);document.documentElement.appendChild(__menu);" +
            "__btn.addEventListener('click',function(){__menu.style.display=__menu.style.display==='none'?'block':'none';});}" +
            "var __menuEl=document.getElementById('__us_menu_list');__menuEl.innerHTML='';" +
            "__ids.forEach(function(__mid){var __cmd=window.__usRuntime.menuCommands[__mid];var __item=document.createElement('div');" +
            "__item.textContent=__cmd.caption;__item.style.cssText='padding:10px 14px;border-bottom:1px solid rgba(255,255,255,0.08);cursor:pointer;';" +
            "__item.addEventListener('click',function(){__menuEl.style.display='none';try{__cmd.onClick();}catch(__e){}});__menuEl.appendChild(__item);});" +
            "}catch(__e){}}" +
            "var GM={" +
            "info:GM_info,log:function(){GM_log.apply(null,arguments);return Promise.resolve();}," +
            "getValue:function(k,d){return Promise.resolve(GM_getValue(k,d));}," +
            "setValue:function(k,v){GM_setValue(k,v);return Promise.resolve();}," +
            "deleteValue:function(k){GM_deleteValue(k);return Promise.resolve();}," +
            "listValues:function(){return Promise.resolve(GM_listValues());}," +
            "addValueChangeListener:function(n,cb){return Promise.resolve(GM_addValueChangeListener(n,cb));}," +
            "removeValueChangeListener:function(id){GM_removeValueChangeListener(id);return Promise.resolve();}," +
            "openInTab:function(u,o){return Promise.resolve(GM_openInTab(u,o));}," +
            "setClipboard:function(t,ty){GM_setClipboard(t,ty);return Promise.resolve();}," +
            "notification:function(d,od){GM_notification(d,od);return Promise.resolve();}," +
            "xmlHttpRequest:function(d){return new Promise(function(res){var __d=Object.assign({},d);var __ol=__d.onload,__oe=__d.onerror;__d.onload=function(r){if(__ol)__ol(r);res(r);};__d.onerror=function(r){if(__oe)__oe(r);res(r);};GM_xmlhttpRequest(__d);});}," +
            "download:function(d,f){GM_download(d,f);return Promise.resolve();}," +
            "getResourceText:function(n){return Promise.resolve(GM_getResourceText(n));}," +
            "getResourceUrl:function(n){return Promise.resolve(GM_getResourceURL(n));}," +
            "registerMenuCommand:function(c,cb,a){return Promise.resolve(GM_registerMenuCommand(c,cb,a));}," +
            "unregisterMenuCommand:function(id){GM_unregisterMenuCommand(id);return Promise.resolve();}" +
            "};" +
            "var GM_xmlHttpRequest=GM_xmlhttpRequest;" +
            requireBlock +
            "function __runUserScript(){try{\n${script.code}\n}catch(__e){console.error('[UserScript] '+__scriptName+' failed',__e);}}" +
            "var __runAt=${quote(meta.runAt)};" +
            "if(__runAt==='document-start'){__runUserScript();}" +
            "else if(__runAt==='document-end'){if(document.readyState!=='loading'){__runUserScript();}else{document.addEventListener('DOMContentLoaded',__runUserScript);}}" +
            "else{if(document.readyState==='complete'){__runUserScript();}else{window.addEventListener('load',__runUserScript);}}" +
            "})();"
    }
}
