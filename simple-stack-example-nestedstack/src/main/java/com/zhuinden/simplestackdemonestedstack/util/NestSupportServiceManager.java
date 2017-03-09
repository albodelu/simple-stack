package com.zhuinden.simplestackdemonestedstack.util;

import android.content.Context;
import android.util.Log;

import com.zhuinden.servicetree.ServiceTree;
import com.zhuinden.simplestack.Backstack;
import com.zhuinden.simplestack.BackstackManager;
import com.zhuinden.simplestack.Bundleable;
import com.zhuinden.simplestack.StateChange;
import com.zhuinden.simplestackdemonestedstack.application.Key;
import com.zhuinden.statebundle.StateBundle;

import java.util.List;

public class NestSupportServiceManager {
    public static final String SERVICE_MANAGER = "SERVICE_MANAGER";

    public static NestSupportServiceManager get(Context context) {
        //noinspection ResourceType
        return (NestSupportServiceManager) context.getSystemService(SERVICE_MANAGER);
    }

    private final ServiceTree serviceTree;

    public NestSupportServiceManager(ServiceTree serviceTree) {
        this.serviceTree = serviceTree;
    }

    public static final String SERVICE_STATES = "SERVICE_BUNDLE";

    private static final String TAG = "NestServiceManager";

    public StateBundle persistStates() {
        StateBundle serviceStates = new StateBundle();
        serviceTree.traverseTree(ServiceTree.Walk.PRE_ORDER, node -> {
            StateBundle keyBundle = new StateBundle();
            for(ServiceTree.Node.Entry entry : node.getBoundServices()) {
                if(entry.getService() instanceof Bundleable) {
                    keyBundle.putParcelable(entry.getName(), ((Bundleable) entry.getService()).toBundle());
                }
            }
            serviceStates.putParcelable(node.getKey().toString(), keyBundle);
        });
        return serviceStates;
    }

    public void setupServices(StateChange stateChange) {
        StateBundle states = serviceTree.getRootService(SERVICE_STATES);
        for(Object _previousKey : stateChange.getPreviousState()) {
            Key previousKey = (Key) _previousKey;
            if(!stateChange.getNewState().contains(previousKey)) {
                ServiceTree.Node previousNode = serviceTree.getNode(previousKey);
                if(states != null) {
                    serviceTree.traverseSubtree(previousNode, ServiceTree.Walk.POST_ORDER, node -> {
                        states.remove(node.getKey().toString());
                        Log.i(TAG, "Destroy [" + node + "]");
                    });
                }
                serviceTree.removeNodeAndChildren(previousNode);
            }
        }
        for(Object _newKey : stateChange.getNewState()) {
            Key newKey = (Key) _newKey;
            buildServices(states, newKey);
        }
    }

    private void buildServices(StateBundle states, Key newKey) {
        if(!serviceTree.hasNodeWithKey(newKey)) {
            ServiceTree.Node.Binder binder;
            if(newKey instanceof Child) {
                binder = serviceTree.createChildNode(serviceTree.getNode(((Child) newKey).parent()), newKey);
            } else {
                binder = serviceTree.createRootNode(newKey);
            }
            buildServicesForKey(states, newKey, binder);
        }
    }

    private void buildServicesForKey(StateBundle states, Key newKey, ServiceTree.Node.Binder binder) {
        newKey.bindServices(binder);
        ServiceTree.Node node = binder.get();
        restoreServiceStateForKey(states, newKey, node);
        if(newKey instanceof Composite) {
            for(Object _nestedKey : ((Composite) newKey).keys()) {
                Key nestedKey = (Key) _nestedKey;
                ServiceTree.Node.Binder nestedBinder = serviceTree.createChildNode(node, nestedKey);
                buildServicesForKey(states, (Key) _nestedKey, nestedBinder);
            }
        }
        if(newKey.hasNestedStack()) {
            Backstack nestedStack = serviceTree.getNode(newKey).<BackstackManager>getService(Key.NESTED_STACK).getBackstack();
            for(Object _childKey : nestedStack.getInitialParameters()) {
                buildServices(states, (Key) _childKey);
            }
        }
    }

    private void restoreServiceStateForKey(StateBundle states, Key key, ServiceTree.Node node) {
        if(states != null) {
            StateBundle keyBundle = states.getParcelable(key.toString());
            if(keyBundle != null) {
                List<ServiceTree.Node.Entry> entries = node.getBoundServices();
                for(ServiceTree.Node.Entry entry : entries) {
                    if(entry.getService() instanceof Bundleable) {
                        ((Bundleable) entry.getService()).fromBundle(keyBundle.getParcelable(entry.getName()));
                    }
                }
            }
        }
    }

    public void setRestoredStates(StateBundle states) {
        serviceTree.registerRootService(SERVICE_STATES, states);
    }

    public ServiceTree getServiceTree() {
        return serviceTree;
    }
}