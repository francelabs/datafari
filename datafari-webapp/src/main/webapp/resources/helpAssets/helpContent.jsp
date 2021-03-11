<%@ page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8"%>
<h1>Datafari Help</h1>
<h2>Basic Search</h2>

<p>
    Basic Search allows you to specify a query that will be searching into the title, path and content of the indexed documents. 
    Words specified in the query are searched altogether. To search for terms individually, use the advanced search or a boolean operator (see below). 
    Basic Search is done through the search bar that is found on the home page and search page of Datafari.
</p>
<p> 
    An autocompletion mechanism will suggest the terms available in the index, proposing terms that start with the letters that you have already typed to help you complete you query
</p>
<p> 
    Basic Search supports the usage of boolean operators such as AND or OR to craft more refined queries. More on that later in this help page.
</p>
<p> 
    If you type in something incorrectly, or if a search query generates very few results, Datafari will suggest another query that may generate more results.
</p>

<h2>Advanced Search</h2>
<p> 
    For a desired query, Advanced Search allows you to specify in which parts of the indexed documents you want to search the terms. 
    You can add more criteria by pressing the "+" button at the bottom of the Advanced Search page. Among the available options, the most notables are:
</p>
<dl>
    <dt>Source:</dt> 
    <dd>
        The source from which the document has been indexed. This is dependent on the configuration that has been used in the indexation jobs. 
    </dd>
    <dt>title:</dt> 
    <dd>
        Allow to search in the titles of the documents
    </dd>
    <dt>url:</dt> 
    <dd>
        Allows to search in the path of the files. Might be useful as an aditional criteria when you know the file should be in a particular subtree. 
    </dd>
    <dt>content_en / content_fr:</dt> 
    <dd>
        Allows to search in the content of the file, either french or english specifically. 
    </dd>
    <dt>Extension :</dt>
    <dd>
        Allows to filter search results to a specific extension (doc, pdf, msg, ...). 
    </dd>
    <dt>File size :</dt> 
    <dd>Allows to specify an interval for the size of the files.</dd>
</dl>
<p> 
    Other options exist and you can find them in the "select a field" dropdown in the advanced search.
</p>

<h2>Using operators :</h2>
<p> 
    You can craft more complex queries using boolean and unary operators. Below is a description of the most common ones :

</p>
<p>
    AND (ou &&): Requires that the terms on both sides of the operator are presents in a document for it to be part of the results.<br>
    This is the default operator in any search you make. It can thus be ommited. "Solar energy" is the same as "solar AND energy" for exemple.
</p>
<p>
    OR : only one of the terms around the operator is needed for a document to be part of the results. 
</p>
<p>
    NOT (or -): the term following this operator must NOT be present in the document for the document to be part of the results.
</p>
<p>   
    +: the term following this operator MUST be part of the document for the document to be part of the results.
</p>
<p>
    The wildcard * is also allowed, which triggers a search for any word that starts with the same characters as the ones declared in the query. <br>
    For instance, searching ret* will return documents containing words such as return, retired, retreat.
</p>
<p>    
    Examples and more operators are presented here: <br/>
    <a target="blank" href="https://lucene.apache.org/solr/guide/7_6/the-standard-query-parser.html#the-standard-query-parser">https://lucene.apache.org/solr/guide/7_6/the-standard-query-parser.html#the-standard-query-parser</a> 
</p>

<h2>Facets:</h2>
<p>
    On the left of the results page, you have a set of filter options called facets. When you click on one of the options, the results are automatically refreshed to match your selection, applying a filter to the current search.
</p>
<p>
    For example, when clicking the "doc" checkbox in the "extension" facet, only .doc documents will be returned as results. This allows you to refine your search and obtain more relevant results.
</p>
