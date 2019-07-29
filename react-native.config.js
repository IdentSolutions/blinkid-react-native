module.exports = {
  dependencies: {
    'blinkid-react-native': {
      platforms: {
        ios: {
          podspecPath: `${__dirname}/node_modules/blinkid-react-native/blinkid-react-native.podspec`,
        }
      },
    },
  },
};
