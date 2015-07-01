/**
 * This file is used to design the desktop interface
 */
$(document).ready(function(){
	if ($(window).width()>799){
		var footerDiv = $('footer');
		var animationCssUp = 'fadeInUP animated';
		var animationCssDown = 'fadeOutDown animated';
		var endAnimation = 'webkitAnimationEnd mozAnimationEnd MSAnimationEnd oanimationend animationend';
		var footer_is_showed = false;
		var timer = null;
		footerDiv.hide();
		 //Firefox
		 $(window).bind('DOMMouseScroll', function(e){
		     if(e.originalEvent.detail > 0) {
		         //scroll down
		         //console.log('Down');
		    	 if (!footer_is_showed){
			    	 footer_is_showed = true;
			    	 footerDiv.show().removeClass(animationCssDown).addClass(animationCssUp).one(endAnimation,function(){
			    		 timer = setTimeout(function(){
			    			footerDiv.removeClass(animationCssUp).addClass(animationCssDown).one(endAnimation,function(){
			    				footerDiv.hide().removeClass(animationCssDown);
			    				footer_is_showed = false;
			    			});
			    		 },1000);
			    	 });
		    	 }else{
		    		 clearTimeout(timer);
		       		 timer = setTimeout(function(){
			    			footerDiv.removeClass(animationCssUp).addClass(animationCssDown).one(endAnimation,function(){
			    				footerDiv.hide().removeClass(animationCssDown);
			    				footer_is_showed = false;
			    			});
		       		 },1000);
		    	 }
		     }else {
		         //scroll up
		     }
		 });
		
		 //IE, Opera, Safari
		 $(window).bind('mousewheel', function(e){
		     if(e.originalEvent.wheelDelta < 0) {
		         //scroll down
		    	 if (!footer_is_showed){
			    	 footer_is_showed = true;
			    	 footerDiv.show().addClass(animationCssUp).one(endAnimation,function(){
			    		 timer = setTimeout(function(){
			    			footerDiv.removeClass(animationCssUp).addClass(animationCssDown).one(endAnimation,function(){
			    				footerDiv.hide().removeClass(animationCssDown);
			    				footer_is_showed = false;
			    			});
			    		 },1000);
			    	 });
		    	 }else{
		    		 clearTimeout(timer);
		       		 timer = setTimeout(function(){
			    			footerDiv.removeClass(animationCssUp).addClass(animationCssDown).one(endAnimation,function(){
			    				footerDiv.hide().removeClass(animationCssDown);
			    				footer_is_showed = false;
			    			});
		       		 },1000);
		    	 }
		     }else {
		         //scroll up
		     }
	 });
	}
});