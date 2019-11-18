import axios from 'axios';
import router from '../router';
import message from '../ui-core/components/message'

let notified = false;

const http = axios.create({
  headers: {
    'X-Requested-With': 'XMLHttpRequest'
  },
  withCredentials: true
});

http.interceptors.response.use(function (response) {
  notified = false;
  return response;
}, function (error) {
  authorizeHandle(error);
  return Promise.reject(error);
});

function authorizeHandle ({ response }) {
  const { status } = response;
  if (status === 401) {
    if (!notified) message.info('Not logging');
    router.push({
      name: 'login'
    })
    notified = true;
  }
}

export default function request (method, path, options, flags = { enable: true }) {
  this.$notice.config({
    duration: 4000
  });

  method = method.toLowerCase();

  if (['post', 'put', 'delete'].includes(method)) {
    options.contentType = 'json';
  }

  let noticeType = '';

  let defaultFlags = {
    enable: true,
    successTitle: 'Operation success',
    successMessage: '',
    failTitle: 'Operation failed',
    failMessage: ''
  };

  if (['post', 'put', 'delete'].includes(method)) {
    defaultFlags.successTitle = 'Operation success';
    defaultFlags.failTitle = 'Operation failed';
    noticeType = '$notice';
  }

  if (method === 'get') {
    defaultFlags.successTitle = 'Request success';
    defaultFlags.failTitle = 'Request failed';
    noticeType = '$notice';
  }

  flags = Object.assign({}, defaultFlags, flags);

  let noticeHandler = (body) => {
    let message = flags.failMessage || body.errmsg || 'Unknow error';

    if (body.errno) {
      this[noticeType].error({
        title: flags.failTitle,
        duration: 0,
        desc: `Error code: ${body.errno} \n ErrorMessage: ${message}`
      });
      throw new Error(body.errmsg);
    } else {
      let isSuccess = body.errno === 0;

      if (method === 'get' && !isSuccess) {
        this[noticeType].error({
          title: flags.failTitle,
          desc: `ErrorMessage: ${message}`,
          duration: 0
        });
      }

      if (['post', 'put', 'delete'].includes(method)) {
        if (isSuccess) {
          this[noticeType].success({
            title: flags.successTitle,
            desc: flags.successMessage
          });
        } else {
          this[noticeType].error({
            title: flags.successTitle,
            desc: flags.successMessage,
            duration: 0
          });
        }
      }
    }
  };

  let promise = http[method](path, options);

  return promise.then((res) => {
    if (flags.enable) {
      noticeHandler(res.data);
    }

    if (res.data.errno === 0) {
      return res.data;
    }
  }).catch(() => {

  });
}
