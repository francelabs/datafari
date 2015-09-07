AjaxFranceLabs.LikesAndFavoritesWidget = AjaxFranceLabs.SubClassResultWidget.extend({

	SERVERALREADYPERFORMED : 1,
	SERVERALLOK : 0,
	SERVERGENERALERROR : -1,
	SERVERNOTCONNECTED : -2,
	SERVERPROBLEMCONNECTIONDB : -3,
	PROBLEMECONNECTIONSERVER : -404,
	SERVERTESTPING : "www.google.com",
	isMobile : $(window).width()<800,

	buildWidget : function() {
		this._super();
		if (!this.isMobile){
			if ($("#containerError").length==0){
				// appending the alert message that will be displayed if there's an error
				$("body").append('<div id="containerError" style="display:none"><div id="smallContainerError"><div id="error_title">Error :</div><span id="messageError"></span></div></div>');
			}
		}
	},
	beforeRequest: function() {
		this._super();
		this.manager.store.get("fl").value = this.manager.store.get("fl").val() + ", nbLikes:field(nbLikes)";
	},
	afterRequest : function() {
		this._super();
		if (!this.isMobile){
			//var isConneted = true;
			var self = this;
			var docs = self.manager.response.response.docs;
			if (window.globalVariableLikes===undefined || window.globalVariableFavorites===undefined){
				// if the likes and Favorites aren't yet gotten from the server
				$.post("./getLikesFavorites",function(data){
					if (data.code==0){
						window.globalVariableLikes = data.likes;
						window.globalVariableFavorites = data.favorites;
						self.afterGettingLikes(docs);
					}
					else if (data.code == self.SERVERNOTCONNECTED ){
					}else{
						// if there's a probleme we CAN'T call self.afterGettingLikes
						self.showError(data.code);
					}
				},"json")
				.fail(function(){
					// if the query failed, then there's two possibilities : Connexion to Internet isn't working 
					// or the server is down.
					self.showError(self.PROBLEMECONNECTIONSERVER );	
				});;
			}else{
				// the Likes and Favorites are already saved (it's the case when we use the pagination or use a facet) 
				self.afterGettingLikes(docs);
			}
		}
	},

	afterGettingLikes : function(docs){
		var self = this;
		if (docs.length!=0){ 
			$(".doc_list .res").append('<span class="favorite"><i class="fa fa-bookmark-o"></i></span>').find(".description")
			.append('<div class="metadonne"><span class="liker">Like</span>  <i class="fa fa-thumbs-up"></i><span class="likes">0</span></div>');
			$(".doc_list > div").data("isLiked",false).data("isFavorite",false);
			$.each(docs,function(index,doc){
				if ($.inArray(docs[index].id,window.globalVariableLikes)!==-1){
					// the document is liked
					$($('.doc_list > div')[index]).data("isLiked",true).find('.liker').text('unlike');
				}
				if ($.inArray(docs[index].id,window.globalVariableFavorites)!==-1){
					// the document is saved as favorite 
					$($('.doc_list > div')[index]).data("isFavorite",true).find('.favorite i').removeClass('fa-bookmark-o').addClass('fa-bookmark');
				}
				// save the number of likes of a document gotten from Solr
				$($('.doc_list > div')[index]).data('likes',doc.nbLikes).find('.likes').text(doc.nbLikes);
			});
		}
		$(".favorite").off("click").on("click",function(){			
			var element = $(this);
			// we need to access to the root of the result where we had saved as data "isFavorite" and "isLiked"
			while(!element.hasClass("res")){
				element = element.parent();
			}
			element = element.parent();
			if (element.data("isFavorite")==true){			
				// if the element was already saved as Favorite, we remove it from favorite (switching the state)
				$.post('./deleteFavorite',{"idDocument":element.attr('id')},function(data){
					if (data.code == self.SERVERALLOK){
						// if the document was deleted from favorites successfully
						window.globalVariableFavorites.splice($.inArray(element.attr('id'), window.globalVariableFavorites),1); // we delete it from window
						element.data("isFavorite",false);
						element.find('.favorite i').removeClass('fa-bookmark').addClass('fa-bookmark-o');
					}else{
						self.showError(data.code);
					}
				},"json")
				.fail(function(){
					// Problem of connexion to the server
					self.showError(self.PROBLEMECONNECTIONSERVER );	
				});;
			}else{
				$.post('./addFavorite',{"idDocument":element.attr('id')},function(data){
					if (data.code == self.SERVERALLOK){
						// if the we saved the document as a Favorite successfully
						window.globalVariableFavorites.push(element.attr('id')); // we add it to window
						element.find('.favorite i').removeClass('fa-bookmark-o').addClass('fa-bookmark');
						element.data("isFavorite",true);
					}else{
						self.showError(data.code);
					}
				},"json")
				.fail(function(){
					self.showError(PROBLEMCONNECTIONSERVER);
				});;
			}
		});		
		$(".liker").off("click").on("click",function(){			
			var element = $(this);
			// as the favorite we need to go to the root
			while(!element.hasClass("res")){
				element = element.parent();
			}
			element = element.parent();
			if (element.data("isLiked")==true){				
				$.post('./unlike',{"idDocument":element.attr('id')},function(data){
					if (data.code >= 0){ 
						// if there wasn't an error 
						window.globalVariableLikes.splice($.inArray(element.attr('id'), window.globalVariableLikes),1);
						element.data("isLiked",false);
						element.find('.liker').text('Like');
						var nbLikes = element.data('likes')-1;
						if (nbLikes<0) nbLikes=0;
						element.data('likes',nbLikes);
						element.find('.likes').text(nbLikes);
					}else{
						self.showError(data.code);
					}
				},"json")
				.fail(function(){
					self.showError(self.PROBLEMECONNECTIONSERVER );	
				});

			}else{
				$.post('./addLikes',{"idDocument":element.attr('id')},function(data){
					if (data.code >= 0){
						window.globalVariableLikes.push(element.attr('id'));
						element.find('.liker').text('Unlike');
						element.data("isLiked",true);
						var nbLikes = element.data('likes')+1;
						element.data('likes',nbLikes);
						element.find('.likes').text(nbLikes);
					}else{
						self.showError(data.code);
					}
				},"json")
				.fail(function(){
					self.showError(self.PROBLEMECONNECTIONSERVER );	
				});;
			}
		});
	},

	showError : function(codeError){
		var messageError;
		switch(codeError){
		case this.SERVERNOTCONNECTED :
			messageError =  "You're not connected yet. Please Reload the page";
			break;
		case this.SERVERPROBLEMCONNECTIONDB:
			messageError = "A problem occured while trying to connect to database. Please retry later"
				break;
		case this.PROBLEMECONNECTIONSERVER:
			messageError = "The server doesn't respound. Please check your connection and retry later";
			break;
		default :
			messageError=  "An undefined problem ocurred. Please retry later by reloading the page";
		break;
		}
		$("#smallContainerError #messageError").text(messageError);
		$("#containerError").show();
	}
});