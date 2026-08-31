<h1>Aide de Datafari</h1>
<h2>Recherche simple</h2>

<p>
    La recherche simple vous permet de saisir une requête qui sera recherchée dans le titre, le chemin et le contenu des documents indexés.
    Les mots indiqués dans la requête sont recherchés ensemble. Pour rechercher les termes individuellement, utilisez la recherche avancée ou un opérateur booléen, voir ci-dessous.
    La recherche simple s’effectue depuis la barre de recherche disponible sur la page d’accueil et sur la page de recherche de Datafari.
</p>
<p>
    Un mécanisme d’autocomplétion vous proposera les termes disponibles dans l’index, en suggérant des termes commençant par les lettres que vous avez déjà saisies afin de vous aider à compléter votre requête.
</p>
<p>
    La recherche simple prend en charge l’utilisation d’opérateurs booléens tels que AND ou OR afin de construire des requêtes plus précises. Vous trouverez plus d’informations à ce sujet plus loin dans cette page d’aide.
</p>
<p>
    Si vous saisissez quelque chose d’incorrect, ou si une requête génère très peu de résultats, Datafari vous proposera une autre requête susceptible de générer davantage de résultats.
</p>

<h2>Recherche avancée</h2>
<p>
    Pour une requête donnée, la recherche avancée vous permet de préciser dans quelles parties des documents indexés vous souhaitez rechercher les termes.
    Vous pouvez ajouter davantage de critères en cliquant sur le bouton « + » en bas de la page de recherche avancée. Parmi les options disponibles, les plus notables sont :
</p>
<dl>
    <dt>Source :</dt>
    <dd>
        La source depuis laquelle le document a été indexé. Cela dépend de la configuration utilisée dans les jobs d’indexation.
    </dd>
    <dt>Titre :</dt>
    <dd>
        Permet de rechercher dans les titres des documents.
    </dd>
    <dt>URL :</dt>
    <dd>
        Permet de rechercher dans le chemin des fichiers. Cela peut être utile comme critère supplémentaire lorsque vous savez que le fichier devrait se trouver dans une arborescence particulière.
    </dd>
    <dt>content_en / content_fr :</dt>
    <dd>
        Permet de rechercher dans le contenu du fichier, spécifiquement en français ou en anglais.
    </dd>
    <dt>Extension :</dt>
    <dd>
        Permet de filtrer les résultats de recherche sur une extension spécifique, par exemple doc, pdf, msg, etc.
    </dd>
    <dt>Taille du fichier :</dt>
    <dd>Permet de définir un intervalle pour la taille des fichiers.</dd>
</dl>
<p>
    D’autres options existent et sont disponibles dans la liste déroulante « sélectionner un champ » de la recherche avancée.
</p>

<h2>Utilisation des opérateurs :</h2>
<p>
    Vous pouvez construire des requêtes plus complexes à l’aide d’opérateurs booléens et unaires. Voici une description des plus courants :
</p>
<p>
    AND ou && : impose que les termes situés de chaque côté de l’opérateur soient présents dans un document pour que celui-ci fasse partie des résultats.<br>
    Il s’agit de l’opérateur par défaut dans toutes les recherches que vous effectuez. Il peut donc être omis. Par exemple, « énergie solaire » équivaut à « énergie AND solaire ».
</p>
<p>
    OR : un seul des termes situés autour de l’opérateur est nécessaire pour qu’un document fasse partie des résultats.
</p>
<p>
    NOT ou - : le terme suivant cet opérateur ne doit PAS être présent dans le document pour que celui-ci fasse partie des résultats.
</p>
<p>
    + : le terme suivant cet opérateur DOIT être présent dans le document pour que celui-ci fasse partie des résultats.
</p>
<p>
    Le caractère générique * est également autorisé. Il déclenche une recherche sur tous les mots qui commencent par les mêmes caractères que ceux indiqués dans la requête.<br>
    Par exemple, rechercher ret* retournera les documents contenant des mots tels que return, retired ou retreat.
</p>

<h2>Facettes :</h2>
<p>
    À gauche de la page de résultats, vous disposez d’un ensemble d’options de filtrage appelées facettes. Lorsque vous cliquez sur l’une de ces options, les résultats sont automatiquement actualisés pour correspondre à votre sélection, en appliquant un filtre à la recherche en cours.
</p>
<p>
    Par exemple, lorsque vous cochez la case « doc » dans la facette « extension », seuls les documents .doc seront retournés dans les résultats. Cela vous permet d’affiner votre recherche et d’obtenir des résultats plus pertinents.
</p>
