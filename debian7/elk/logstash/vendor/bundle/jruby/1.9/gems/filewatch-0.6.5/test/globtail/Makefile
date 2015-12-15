test:
	@i=1; while [ -x "test$${i}.sh" ]; do \
		echo "test$${i}:" ; \
		./test$${i}.sh || exit 1 ; \
		echo "PASS" ; \
		i=$$(($$i+1)) ; \
	done
