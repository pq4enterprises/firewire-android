package com.pioneer.nycfirewire.extensions

import androidx.lifecycle.MutableLiveData
import com.pioneer.nycfirewire.resource.Resource
import com.pioneer.nycfirewire.resource.ResourceState


fun <T> MutableLiveData<Resource<T>>.setSuccess(data: T? = null) {
    postValue(Resource(ResourceState.SUCCESS, data))
}

fun <T> MutableLiveData<Resource<T>>.setLoading() {
     postValue(Resource(ResourceState.LOADING, value?.data))
}

fun <T> MutableLiveData<Resource<T>>.setError(message: String? = null) {
    postValue(Resource(ResourceState.ERROR, value?.data, message))
}