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
		footerDiv.hide();
		 //Firefox
		 $(window).bind('DOMMouseScroll', function(e){
		     if(e.originalEvent.detail > 0) {
		         //scroll down
		         //console.log('Down');
		    	 if (!footer_is_showed){
			    	 footer_is_showed = true;
		    		 clearTimeout(window.globtimer);
			    	 footerDiv.off(endAnimation).removeClass(animationCssDown).addClass(animationCssUp).show();
			    	 footerDiv.one(endAnimation,function(){
			    		 window.globtimer = setTimeout(function(){
			    			footerDiv.off(endAnimation).removeClass(animationCssUp).addClass(animationCssDown);
			    			footerDiv.one(endAnimation,function(){
			    				footerDiv.hide().removeClass(animationCssDown);
			    				footer_is_showed = false;
			    			});
			    		 },1000);
			    	 });
		    	 }else{
		    		 delay_hide();
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
		    		 clearTimeout(window.globtimer);
			    	 footer_is_showed = true;
			    	 footerDiv.off(endAnimation).removeClass(animationCssDown).addClass(animationCssUp).show();
			    	 footerDiv.one(endAnimation,function(){
			    		 window.globtimer = setTimeout(function(){
			    			footerDiv.off(endAnimation).removeClass(animationCssUp).addClass(animationCssDown);
			    			footerDiv.one(endAnimation,function(){
			    				footerDiv.hide().removeClass(animationCssDown);
			    				footer_is_showed = false;
			    			});
			    		 },1000);
			    	 });
		    	 }else{
		    		delay_hide()
		    	 }
		     }else {
		         //scroll up
		     }
	 });
		 function delay_hide(){
			 clearTimeout(window.globtimer);
       		 window.globtimer = setTimeout(function(){
	    			footerDiv.off(endAnimation).removeClass(animationCssUp).addClass(animationCssDown);
	    			footerDiv.one(endAnimation,function(){
	    				footerDiv.hide().removeClass(animationCssDown);
	    				footer_is_showed = false;
	    			});
       		 },1000);
		 }
		 $('footer').mouseover(function(){
			 footer_is_showed=true;
			 clearTimeout(window.globtimer);
			 $('footer').mouseout(function(){
				 delay_hide();
			 });
		 });
	}
});