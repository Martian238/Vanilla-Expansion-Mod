Time.run(1, run(() => {
	let allContent = Vars.content.getContentMap();
	for(let j = 0; j < allContent.length; j++){
		let array = allContent[j];
		if(array.size == 0) continue;
		for(let c of array.toArray()){
			if(c instanceof UnlockableContent) c.unlock();
		}
	}
	Vars.ui.showInfo('[red]测试版特供\n已解锁全科技');
}));
