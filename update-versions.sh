sed -E -i "s/(<)?(version)([:' >]+)?([0-9]\.[0-9]\.[0-9](-[a-z0-9]+\.?[0-9]*)?)(['<]?)/\1\2\3$1\6/g" README.md docs/admin/README.md docs/developer/README.md