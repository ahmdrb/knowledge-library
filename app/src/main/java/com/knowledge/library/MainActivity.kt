package com.knowledgelibrary

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class Node(val id:String=UUID.randomUUID().toString(), var title:String, var starred:Boolean=false, val children:MutableList<Node> = mutableListOf())
data class LibraryPackage(val id:String=UUID.randomUUID().toString(), var title:String, val nodes:MutableList<Node> = mutableListOf())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { MaterialTheme { LibraryApp(this) } } }
}

private fun parseOutline(raw:String): LibraryPackage? {
    val lines=raw.removePrefix("\uFEFF").lines().filter { it.isNotBlank() }
    if(lines.isEmpty()) return null
    val pkg=LibraryPackage(title=lines.first().trim())
    val stack=mutableListOf<Pair<Int,Node>>()
    for(line in lines.drop(1)) {
        val spaces=line.takeWhile { it==' ' }.length
        val level=spaces/4
        val title=line.trim().replace(Regex("^\\d+[.)]\\s*"),"").trim()
        if(title.isEmpty()) continue
        val node=Node(title=title)
        while(stack.isNotEmpty() && stack.last().first>=level) stack.removeAt(stack.lastIndex)
        if(stack.isEmpty()) pkg.nodes.add(node) else stack.last().second.children.add(node)
        stack.add(level to node)
    }
    return pkg
}
private fun nodeJson(n: Node): JSONObject {
    val children = JSONArray()
    for (child in n.children) {
        children.put(nodeJson(child))
    }

    return JSONObject()
        .put("id", n.id)
        .put("title", n.title)
        .put("starred", n.starred)
        .put("children", children)
}
private fun packageJson(p:LibraryPackage)=JSONObject().put("id",p.id).put("title",p.title).put("nodes",JSONArray().also { a->p.nodes.forEach { a.put(nodeJson(it)) } })
private fun readNode(o:JSONObject):Node { val n=Node(o.optString("id",UUID.randomUUID().toString()),o.optString("title","Untitled"),o.optBoolean("starred")); val a=o.optJSONArray("children")?:JSONArray(); for(i in 0 until a.length()) n.children.add(readNode(a.getJSONObject(i))); return n }
private fun readPackage(o:JSONObject):LibraryPackage { val p=LibraryPackage(o.optString("id",UUID.randomUUID().toString()),o.optString("title","Untitled")); val a=o.optJSONArray("nodes")?:JSONArray(); for(i in 0 until a.length()) p.nodes.add(readNode(a.getJSONObject(i))); return p }
private fun load(context: Context): MutableList<LibraryPackage> {
    return try {
        val raw: String =
            context.getSharedPreferences("knowledge-library-native", 0)
                .getString("packages", "[]") ?: "[]"

        val array = JSONArray(raw)
        val result: MutableList<LibraryPackage> = mutableListOf()

        for (index in 0 until array.length()) {
            val item: JSONObject = array.getJSONObject(index)
            val libraryPackage: LibraryPackage = readPackage(item)
            result.add(libraryPackage)
        }

        result
    } catch (_: Exception) {
        mutableListOf<LibraryPackage>()
    }
}
private fun save(context:Context,packages:List<LibraryPackage>) { val a=JSONArray(); packages.forEach { a.put(packageJson(it)) }; context.getSharedPreferences("knowledge-library-native",0).edit().putString("packages",a.toString()).apply() }

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun LibraryApp(context:Context) {
    val packages= remember { mutableStateListOf<LibraryPackage>().also { it.addAll(load(context)) } }
    var query by remember { mutableStateOf("") }; var selected by remember { mutableStateOf<String?>(null) }; var showAdd by remember { mutableStateOf(false) }; var title by remember { mutableStateOf("") }; var outline by remember { mutableStateOf("") }; var showOutline by remember { mutableStateOf(false) }; var favoritesOnly by remember { mutableStateOf(false) }; var message by remember { mutableStateOf<String?>(null) }
    fun persist()=save(context,packages)
    val importLauncher= rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri:Uri? -> if(uri!=null) try { val text=context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: error("Could not read file"); val p=parseOutline(text) ?: error("The file is empty"); val index=packages.indexOfFirst { it.title.equals(p.title,true) }; if(index>=0){ preserveStars(packages[index].nodes,p.nodes); packages[index]=LibraryPackage(packages[index].id,p.title,p.nodes) } else packages.add(p); persist(); selected=packages.first { it.title.equals(p.title,true) }.id; message="Imported ${p.title}" } catch(e:Exception) { message="Import failed: ${e.message}" } }
    val backupImport= rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri!=null) try {
            val text=context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: error("Could not read the selected file")
            val root=JSONObject(text.removePrefix("\uFEFF"))
            require(root.optString("format")=="knowledge-library-backup") { "Unsupported backup format. Select a Knowledge Library JSON backup." }
            require(root.optInt("version",-1)==1) { "Unsupported backup version: ${root.optInt("version",-1)}" }
            val array=root.optJSONArray("packages") ?: error("Backup is missing its packages list")
            val imported=mutableListOf<LibraryPackage>()
            val seen=mutableSetOf<String>()
            for(i in 0 until array.length()) {
                val item=array.optJSONObject(i) ?: error("Package ${i+1} is invalid")
                val packageTitle=item.optString("title").trim()
                require(packageTitle.isNotEmpty()) { "Package ${i+1} has no title" }
                require(seen.add(packageTitle.lowercase())) { "Duplicate package title: $packageTitle" }
                require(item.optJSONArray("nodes")!=null) { "Package '$packageTitle' has no concepts list" }
                imported.add(readPackage(item))
            }
            // Parse and validate the entire file before changing any existing data.
            imported.forEach { p ->
                val index=packages.indexOfFirst { it.title.equals(p.title,true) }
                if(index>=0) packages[index]=p else packages.add(p)
            }
            persist(); message="Imported ${imported.size} package(s) from backup"
        } catch(e:Exception) { message="Backup import failed: ${e.message ?: "Invalid file"}" }
    }
    val backupExport=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")){uri->
        if(uri!=null) try {
            val root=JSONObject().put("format","knowledge-library-backup").put("version",1).put("exportedAt",java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.US).format(java.util.Date())).put("packages",JSONArray().also{a->packages.forEach{a.put(packageJson(it))}})
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use{it.write(root.toString(2))} ?: error("Could not write the selected file")
            message="Backup exported"
        }catch(e:Exception){message="Export failed: ${e.message}"}
    }
    val current=packages.firstOrNull{it.id==selected}
    Scaffold(topBar={ TopAppBar(title={ Text("Knowledge Library") }, navigationIcon={ if(current!=null) TextButton(onClick={selected=null}){Text("Back")} }) }, floatingActionButton={ if(current==null) FloatingActionButton(onClick={showAdd=true}){Text("+")} }) { pad ->
        Column(Modifier.fillMaxSize().padding(pad).padding(horizontal=16.dp)) {
            if(current==null) {
                OutlinedTextField(value=query,onValueChange={query=it},modifier=Modifier.fillMaxWidth(),label={Text("Search packages and concepts")},singleLine=true)
                Row(verticalAlignment=Alignment.CenterVertically){ Text("Favorites",Modifier.weight(1f)); Switch(checked=favoritesOnly,onCheckedChange={favoritesOnly=it}) }
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ OutlinedButton(onClick={importLauncher.launch(arrayOf("text/plain","application/octet-stream","*/*"))}){Text("Import .txt")}; OutlinedButton(onClick={backupImport.launch(arrayOf("application/json","*/*"))}){Text("Import backup")}; OutlinedButton(onClick={backupExport.launch("knowledge-library-backup.json")}){Text("Export") } }
                if(packages.isEmpty()) Text("Import a UTF-8 outline to start your library.",Modifier.padding(20.dp))
                LazyColumn { items(packages.filter { p -> (!favoritesOnly || countStars(p.nodes)>0) && (p.title.contains(query,true) || findNodeTitle(p.nodes,query) || query.isBlank()) },key={it.id}) { p -> Card(Modifier.fillMaxWidth().padding(vertical=5.dp).clickable { selected=p.id }) { Column(Modifier.padding(16.dp)) { Text(p.title,style=MaterialTheme.typography.titleMedium); Text("${p.nodes.size} top-level concepts · ${countNodes(p.nodes)} concepts",style=MaterialTheme.typography.bodySmall); Row { TextButton(onClick={selected=p.id}){Text("Open")}; TextButton(onClick={packages.remove(p);persist()}){Text("Delete")} } } } } }
            } else {
                Text(current.title,style=MaterialTheme.typography.headlineSmall,modifier=Modifier.padding(vertical=8.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){ OutlinedButton(onClick={showOutline=true;outline=toOutline(current)}){Text("Edit outline")}; OutlinedButton(onClick={title="";showAdd=true}){Text("Add concept")} }
                LazyColumn { items(current.nodes,key={it.id}) { node -> NodeRow(node,0,{node.starred=!node.starred;persist()},{ n -> current.nodes.remove(n);persist() }, { persist() }) } }
            }
            message?.let { Text(it, color=MaterialTheme.colorScheme.primary,modifier=Modifier.clickable{message=null}.padding(8.dp)) }
        }
    }
    if(showAdd) AlertDialog(onDismissRequest={showAdd=false},title={Text(if(current==null) "Add package" else "Add top-level concept")},text={OutlinedTextField(value=title,onValueChange={title=it},label={Text("Title")})},confirmButton={TextButton(onClick={if(title.isNotBlank()){ if(current==null) packages.add(LibraryPackage(title=title.trim())) else current.nodes.add(Node(title=title.trim()));persist();title="";showAdd=false }}){Text("Save")}},dismissButton={TextButton(onClick={showAdd=false}){Text("Cancel")}})
    if(showOutline && current!=null) AlertDialog(onDismissRequest={showOutline=false},title={Text("Edit outline")},text={Column { Text("Use four spaces per hierarchy level.",style=MaterialTheme.typography.bodySmall); OutlinedTextField(value=outline,onValueChange={outline=it},modifier=Modifier.fillMaxWidth().height(320.dp), minLines=10) }},confirmButton={TextButton(onClick={val p=parseOutline(outline); if(p!=null){ val idx=packages.indexOfFirst{it.id==current.id}; if(idx>=0){preserveStars(current.nodes,p.nodes); packages[idx]=LibraryPackage(current.id,p.title,p.nodes); persist();showOutline=false }else message="Package no longer exists"}else message="Outline is empty"}){Text("Save")}},dismissButton={TextButton(onClick={showOutline=false}){Text("Cancel")}})
}
@Composable private fun NodeRow(node:Node,depth:Int,onStar:()->Unit,onDelete:(Node)->Unit,onChanged:()->Unit){ var expanded by remember { mutableStateOf(true) }; Column(Modifier.fillMaxWidth().padding(start=(depth*16).dp,top=4.dp,bottom=4.dp)) { Row(verticalAlignment=Alignment.CenterVertically){ if(node.children.isNotEmpty()) TextButton(onClick={expanded=!expanded}){Text(if(expanded) "−" else "+")}; Text(node.title,Modifier.weight(1f)); TextButton(onClick=onStar){Text(if(node.starred) "★" else "☆")}; TextButton(onClick={onDelete(node)}){Text("×")} }; if(expanded) node.children.forEach { child -> NodeRow(child,depth+1,{child.starred=!child.starred;onChanged()}, { node.children.remove(it);onChanged() },onChanged) } } }
private fun countNodes(nodes:List<Node>):Int=nodes.sumOf{1+countNodes(it.children)}
private fun countStars(nodes:List<Node>):Int=nodes.sumOf{(if(it.starred)1 else 0)+countStars(it.children)}
private fun findNodeTitle(nodes:List<Node>,q:String):Boolean=nodes.any{it.title.contains(q,true)||findNodeTitle(it.children,q)}
private fun preserveStars(oldNodes:List<Node>,newNodes:List<Node>) {
    val oldByTitle=oldNodes.groupBy { it.title.trim().lowercase() }
    newNodes.forEach { node ->
        val previous=oldByTitle[node.title.trim().lowercase()]?.firstOrNull()
        if(previous!=null) { node.starred=node.starred || previous.starred; preserveStars(previous.children,node.children) }
    }
}
private fun toOutline(p:LibraryPackage):String { val out=StringBuilder(p.title+"\n"); fun walk(nodes:List<Node>,level:Int){nodes.forEach{out.append("    ".repeat(level)).append(it.title).append('\n');walk(it.children,level+1)}};walk(p.nodes,0);return out.toString() }
