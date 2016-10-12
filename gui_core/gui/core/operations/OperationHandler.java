package gui.core.operations;

import gui.is.services.LoggerDisplayerSvc;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

public abstract class OperationHandler
{
	@Resource(name = "loggerDisplayerSvc")
	private LoggerDisplayerSvc loggerDisplayerSvc;
	
	protected OperationHandler next;
	
	public void setNext(OperationHandler next)
	{
		this.next = next;
	}

	public boolean go() throws InterruptedException {
		if (next != null)
			return next.go();
		
		loggerDisplayerSvc.logGeneral("Last Phase done");
		return true;
	}
}
